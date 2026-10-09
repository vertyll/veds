# Service Dependencies

What each service needs to build and to run, and what it does **not** need. The short answer:
no service calls another to serve a request, so any one of them can be down without taking the others with it — but
"autonomous" is not the same as "standalone", and the difference is below.

## Build-time

Composite-build dependencies, from each service's `settings.gradle.kts`.

| Service              | Shared libraries                                                                                             | Contract modules                                                                                   |
|----------------------|--------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| api-gateway          | `shared-web`                                                                                                 | —                                                                                                  |
| iam-service          | `shared-web`, `shared-translation-client`, `shared-authz`, `shared-messaging-kafka`                          | `iam-contracts`, `mail-contracts`                                                                  |
| mail-service         | `shared-saga-api`, `shared-web`, `shared-translation-client`, `shared-saga-engine`, `shared-messaging-kafka` | `mail-contracts`, `iam-contracts`                                                                  |
| project-service      | `shared-saga-api`, `shared-web`, `shared-translation-client`, `shared-saga-engine`, `shared-messaging-kafka` | `project-contracts`, `mail-contracts`, `iam-contracts`                                             |
| task-service         | `shared-web`, `shared-translation-client`, `shared-messaging-kafka`                                          | `task-contracts`, `project-contracts`, `iam-contracts`, `file-contracts`                           |
| notification-service | `shared-web`, `shared-translation-client`, `shared-messaging-kafka`                                          | `notification-contracts`, `project-contracts`, `task-contracts`, `mail-contracts`, `iam-contracts` |
| translation-service  | `shared-web`, `shared-translation`                                                                           | —                                                                                                  |
| file-service         | `shared-web`, `shared-translation-client`, `shared-messaging-kafka`                                          | `file-contracts`                                                                                   |

A `-contracts` dependency means the service **reads or writes that context's events**, nothing more: it pulls in
generated Avro classes, not code. `task-service` depending on
`project-contracts` does not let it call project-service; it lets it deserialize the events project-service publishes.

### What each shared library is for

The split follows two rules: a module is framework-free when the application layer needs it, and a module is separate
only when some consumer takes it without the rest.

| Library                     | Framework | Taken by | Holds                                                                         |
|-----------------------------|-----------|----------|-------------------------------------------------------------------------------|
| `shared-saga-api`           | none      | 6        | `Saga`, `SagaStep`, `SagaStatus`, `SagaStepStatus`, `SagaTypeValue`           |
| `shared-translation`        | none      | 7        | Key-declaration DSL and the ICU message renderer                              |
| `shared-web`                | Spring    | **9**    | Keycloak JWT converters (servlet + reactive), ETag/optimistic-locking helpers |
| `shared-messaging-kafka`    | Spring    | 7        | Transactional outbox, idempotent consumption, Avro serialisation              |
| `shared-saga-engine`        | Spring    | 6        | One service's local saga: state machine, compensation, watchdog, JPA entities |
| `shared-translation-client` | Spring    | 7        | Start-up registration of a service's translation keys                         |

`shared-saga-api` exists because the application layer of the services that own a saga step speaks saga vocabulary and
must never see a framework — the module boundary turns that rule into a compile error instead of a check. There is
deliberately no `shared-messaging-api`: nothing in any application layer imports the outbox types, so an api/engine
split there would produce a module with exactly the same consumers as the engine.

`shared-web` is the only library the gateway takes. It is reactive and has no database, so it must not inherit JPA —
which a library carrying persistence would force on it.

## Infrastructure at runtime

| Service              | Postgres               | Kafka + Schema Registry | Keycloak             | Other            |
|----------------------|------------------------|-------------------------|----------------------|------------------|
| api-gateway          | —                      | —                       | required             | Redis (sessions) |
| iam-service          | `iam_db`               | required                | required (admin API) | —                |
| mail-service         | `mail_service`         | required                | required (JWT)       | SMTP             |
| project-service      | `project_service`      | required                | required (JWT)       | —                |
| task-service         | `task_service`         | required                | required (JWT)       | —                |
| notification-service | `notification_service` | required                | required (JWT)       | —                |
| translation-service  | `translation_service`  | **not needed**          | required (JWT)       | —                |
| file-service         | `file_service`         | required                | required (JWT)       | Garage (S3 API)  |

Kafka is required wherever `shared-messaging-kafka` is: the outbox dispatcher and, through it, saga compensation.
translation-service publishes nothing and consumes nothing, so it takes neither that module nor the saga engine, and
carries no outbox or saga tables at all. file-service takes the outbox but runs no saga, so it has the outbox tables
and not the saga ones.

## Service-to-service

**No service calls another to serve a request.** There is exactly one synchronous call in the system, and it is not on a
request path:

| Caller        | Callee              | When                                              | If it fails                       |
|---------------|---------------------|---------------------------------------------------|-----------------------------------|
| every service | translation-service | once at start-up, to publish its translation keys | logged; the service starts anyway |

Everything else travels through Kafka. See the [Event Catalogue](./events.md) for who publishes and who consumes what.

## What that buys, and what it costs

A service keeps working when the ones it hears from are down, because it holds its own projection of what it needs —
`task-service` renders a board with project names and assignee names without asking anybody.

The cost is stated rather than hidden: those projections are **eventually consistent**. A member removed from a project
a second ago may still pass an authorization check in task-service until the event arrives. The alternative — asking
project-service on every task read — would couple the availability of the two services, which is the thing this design
is spending consistency to avoid.

## Start-up order

Only one ordering matters, and it is a convenience rather than a requirement: starting translation-service first avoids
a failed registration in the other services' logs. Registration failure is deliberately non-fatal and the keys are
republished on the next restart.

Everything else is `depends_on` in `docker-compose.local.yml`, which waits on health checks.
