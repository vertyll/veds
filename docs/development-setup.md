# Development Setup

Getting the whole system running locally, from a fresh clone.

## Prerequisites

- Docker or Podman, with Compose
- JDK 25 LTS
- Python 3 — only for registering Avro schemas

## 1. Clone and configure

```bash
git clone https://github.com/vertyll/veds.git
cd veds
```

There is nothing to configure. Local defaults live next to the thing that needs them — `docker-compose.local.yml` for the
infrastructure, `application-local.yml` for the gateway — so a fresh clone runs as-is.

Only the `prod` profile demands real values, and it takes them from the environment with no fallback:

| Variable                         | Used by                                                               |
|----------------------------------|-----------------------------------------------------------------------|
| `REDIS_PASSWORD`                 | `application-prod.yml`, and `docker-compose.local.yml` as an override |
| `GATEWAY_SESSION_ENCRYPTION_KEY` | `application-prod.yml` — must decode to 32 bytes                      |

```bash
openssl rand -base64 32   # GATEWAY_SESSION_ENCRYPTION_KEY
```

## 2. Start the infrastructure

```bash
docker compose -f docker-compose.local.yml up -d
```

Every `docker compose` command in this document works verbatim as `podman compose` — the
compose file uses nothing Docker-specific.

This brings up PostgreSQL (one database per service), Keycloak, Kafka with Schema Registry, Redis, Garage and MailDev.
Three one-shot jobs run automatically and then exit — they are supposed to:

- **`topics-init`** applies `infra/kafka/topics.tf`, creating every topic
- **`schemas-init`** registers every Avro schema under `contracts/` with the Schema Registry
- **`object-storage-init`** gives Garage its cluster layout, bucket, access key and CORS rules

None of this can be expressed in a configuration file: it is cluster state. `topics-init` applies it with
Terraform, `schemas-init` with `scripts/schema_registry/register_schemas.py`, `object-storage-init` through the
Garage admin API.

Wait for the health checks before moving on:

```bash
docker compose -f docker-compose.local.yml ps
```

## 3. Re-register the Avro schemas

`schemas-init` registers the schemas on every `up`. After changing a schema, register it again without restarting
the stack:

```bash
python scripts/schema_registry/register_schemas.py --registry-url http://localhost:8081
```

Producers register on first publish, but doing it up front means an incompatible schema is caught now rather than at
runtime, and consumers can start in any order.

## 4. Build

```bash
./gradlew clean build
```

The root project is a composite build aggregating every module. It also exposes `ktlintCheck`,
`ktlintFormat`, `detekt`, `test` and `checkHexagonalDependencies` across all included builds; `check` runs the
verification tasks together. `-contracts` modules are excluded from those aggregators because they contain only
generated Avro classes, and `checkHexagonalDependencies` covers the `-service` builds only — `api-gateway` and the
`shared-*` libraries have no application layer to check.

To build one service on its own:

```bash
cd <service-name> && ./gradlew build
```

## 5. Run the services

Each in its own terminal, or through the `.run` configurations in IntelliJ (`All_services.run.xml` starts everything):

```bash
cd <service-name>
./gradlew bootRun --args='--spring.profiles.active=local'
```

> [!NOTE]
>
> **Order matters in one place only.** Every service registers its translation keys with `translation-service` at
> start-up, so starting that one first avoids a failed registration in the logs. Nothing breaks if you do not:
> registration failure is deliberately non-fatal, and the keys are republished on the next restart.

| Service                | Port |
|------------------------|------|
| `api-gateway`          | 8080 |
| `iam-service`          | 8082 |
| `mail-service`         | 8083 |
| `project-service`      | 8084 |
| `task-service`         | 8085 |
| `notification-service` | 8086 |
| `translation-service`  | 8087 |
| `file-service`         | 8088 |

`template-service` is a reference for cloning and is not meant to be run.

## Service URLs

| Component              | URL                   |
|------------------------|-----------------------|
| API Gateway            | http://localhost:8080 |
| Front end              | http://localhost:4200 |
| Keycloak               | http://localhost:9000 |
| Kafka UI               | http://localhost:8090 |
| Schema Registry        | http://localhost:8081 |
| MailDev                | http://localhost:1080 |
| Object storage (S3)    | http://localhost:9100 |
| Object storage console | http://localhost:9101 |

Databases are exposed on 5432 (iam), 5433 (mail), 5434 (keycloak), 5435 (project), 5436 (task), 5437 (notification),
5438 (translation), 5439 (file).

### Re-running after a contract change

`schemas-init` fails on a second `compose up` if a schema was reshaped — a renamed namespace or a changed field type is,
correctly, incompatible with what the registry already holds. Locally the registry is disposable:

```bash
python scripts/schema_registry/register_schemas.py --registry-url http://localhost:8081 --reset
```

> [!WARNING]
>
> `--reset` drops each subject before registering. Never use it against a shared registry — there the refusal is the
> feature.

## Tests

`./gradlew build` runs the unit tests only. The integration tests need a container runtime and are tagged out of the
default build:

```bash
./gradlew test -PintegrationTests
```

The flag widens what each build runs; every included build takes part either way. `shared-messaging-kafka` has
integration tests of its own, so narrowing by name would silently skip them.

Under Podman they also need `DOCKER_HOST` and `TESTCONTAINERS_RYUK_DISABLED` — see
[Testing](./testing.md).

## API documentation

Each service serves its own Swagger UI at `/swagger-ui.html` — for example
`http://localhost:8084/swagger-ui.html` for project-service.

Library API documentation is generated with Dokka:

```bash
./gradlew docs   # output in docs/dokka/
```

An Insomnia collection is provided at `insomnia-collection.yaml`.

## Monitoring

Every service exposes Spring Boot Actuator at `/actuator/health`.

## Code style

```bash
./gradlew ktlintFormat   # format
./gradlew ktlintCheck    # verify
./gradlew detekt         # static analysis
```

`check` additionally runs `checkHexagonalDependencies`, which fails the build if a framework reaches a service's
application layer. See [Hexagonal Layering](./hexagonal-layering.md).

## Troubleshooting

| Symptom                                           | Cause                                                                                               |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Gateway cannot authenticate against Redis         | `REDIS_PASSWORD` exported for compose but not for `bootRun`, or the reverse — the two then disagree |
| Gateway exits at start-up complaining about a key | `GATEWAY_SESSION_ENCRYPTION_KEY` overridden with a value that is not 32 base64-decoded bytes        |
| Uploads fail in the browser with a CORS error     | `FRONTEND_ORIGIN` does not match where the SPA runs; re-run `object-storage-init`                   |
| The UI shows keys such as `project.not_found`     | `translation-service` is not running, or the services started before it and have not been restarted |
| A consumer logs a schema error                    | Step 3 was skipped                                                                                  |
| Login redirects but never returns                 | Keycloak is not healthy yet, or the realm import has not finished                                   |
