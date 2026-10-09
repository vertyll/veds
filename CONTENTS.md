# Contents

Every document in this repository, the module it belongs to, and what it covers. Terms are defined in
[GLOSSARY.md](GLOSSARY.md), and the specifications behind them are in [STANDARDS.md](STANDARDS.md).

Start with [Development Setup](docs/development-setup.md), then [Architecture](docs/architecture.md).

## Start here

| Document                  | Module | Kind              | Covers                                                         |
|---------------------------|--------|-------------------|----------------------------------------------------------------|
| [veds](README.md)         | —      | repository README | What the repository is, its stack and where to start.          |
| [Glossary](GLOSSARY.md)   | —      | reference         | Every term the docs use, and where it is explained.            |
| [Standards](STANDARDS.md) | —      | reference         | The RFCs and specifications the code implements or depends on. |

## Overview

| Document                                             | Module | Kind     | Covers                                                                                        |
|------------------------------------------------------|--------|----------|-----------------------------------------------------------------------------------------------|
| [Development Setup](docs/development-setup.md)       | —      | overview | Running the whole system locally.                                                             |
| [Testing](docs/testing.md)                           | —      | overview | The two test tiers and the architecture check.                                                |
| [Architecture](docs/architecture.md)                 | —      | overview | Components and design principles.                                                             |
| [Hexagonal Layering](docs/hexagonal-layering.md)     | —      | overview | The dependency rule and how it is enforced.                                                   |
| [Shared Modules](docs/shared-modules.md)             | —      | overview | What each shared library is responsible for.                                                  |
| [Service Dependencies](docs/service-dependencies.md) | —      | overview | What each service needs to build and to run.                                                  |
| [Eventual Consistency](docs/eventual-consistency.md) | —      | overview | Why no transaction spans two services, and the building blocks that carry state between them. |
| [Event Catalog](docs/events.md)                      | —      | overview | Every topic, its owner and its consumers.                                                     |
| [Keycloak Configuration](docs/keycloak.md)           | —      | overview | Realm setup, authentication flow, role management.                                            |
| [Authorization](docs/authorization.md)               | —      | overview | Permissions, roles and how services check them.                                               |

## Mechanisms

| Document                                                         | Module | Kind      | Covers                                                                                                |
|------------------------------------------------------------------|--------|-----------|-------------------------------------------------------------------------------------------------------|
| [CQRS](docs/mechanisms/cqrs.md)                                  | —      | mechanism | Where command/query separation is applied, and why.                                                   |
| [Concurrency Control](docs/mechanisms/optimistic-concurrency.md) | —      | mechanism | Optimistic locking, ETags, saga and outbox concurrency.                                               |
| [Files](docs/mechanisms/files.md)                                | —      | mechanism | Pre-signed uploads, a private bucket and the two sweeps.                                              |
| [Translations](docs/mechanisms/translations.md)                  | —      | mechanism | Key ownership and ICU.                                                                                |
| [Inbox](docs/mechanisms/inbox.md)                                | —      | mechanism | How a consumer handles each event once, even though Kafka delivers at least once.                     |
| [Sagas](docs/mechanisms/sagas.md)                                | —      | mechanism | How a workflow spanning services completes or is compensated, without a central orchestrator.         |
| [Transactional outbox](docs/mechanisms/transactional-outbox.md)  | —      | mechanism | How a state change and the event announcing it commit together, and how the event then reaches Kafka. |

## Services

| Document                                                                           | Module                 | Kind          | Covers                                                                                                |
|------------------------------------------------------------------------------------|------------------------|---------------|-------------------------------------------------------------------------------------------------------|
| [api-gateway](api-gateway/README.md)                                               | `api-gateway`          | module README | The single entry point, and the only component that holds tokens.                                     |
| [Session](api-gateway/docs/mechanisms/session.md)                                  | `api-gateway`          | mechanism     | How the gateway turns an opaque cookie into the person's token, and keeps that safe.                  |
| [Token exchange](api-gateway/docs/mechanisms/token-exchange.md)                    | `api-gateway`          | mechanism     | How each service receives a token meant for it alone, and why a token leaked from one opens no other. |
| [Token refresh](api-gateway/docs/mechanisms/token-refresh.md)                      | `api-gateway`          | mechanism     | How the session keeps a valid access token without signing the person out when requests race.         |
| [file-service](file-service/README.md)                                             | `file-service`         | module README | File metadata and signed URLs.                                                                        |
| [File scopes](file-service/docs/mechanisms/file-scopes.md)                         | `file-service`         | mechanism     | How "how big may this be" has one answer per kind of file.                                            |
| [iam-service](iam-service/README.md)                                               | `iam-service`          | module README | Users, roles, permissions and the account lifecycle.                                                  |
| [Second factor](iam-service/docs/mechanisms/second-factor.md)                      | `iam-service`          | mechanism     | How a person enables and disables a second factor without a secret passing through the application.   |
| [User provisioning](iam-service/docs/mechanisms/user-provisioning.md)              | `iam-service`          | mechanism     | How iam-service learns about a person Keycloak already knows.                                         |
| [mail-service](mail-service/README.md)                                             | `mail-service`         | module README | Delivers e-mail on request and reports the outcome.                                                   |
| [notification-service](notification-service/README.md)                             | `notification-service` | module README | In-app notifications and their delivery preferences.                                                  |
| [Real-time transport](notification-service/docs/mechanisms/real-time-transport.md) | `notification-service` | mechanism     | How a new notification reaches an open browser.                                                       |
| [project-service](project-service/README.md)                                       | `project-service`      | module README | Projects, membership and per-project authorization.                                                   |
| [Access policy](project-service/docs/mechanisms/access-policy.md)                  | `project-service`      | mechanism     | How project-service decides what a person may do with a project.                                      |
| [Localized labels](project-service/docs/mechanisms/localized-labels.md)            | `project-service`      | mechanism     | How category and status names written by users are stored and served in several languages.            |
| [task-service](task-service/README.md)                                             | `task-service`         | module README | Tasks, comments and the board.                                                                        |
| [Board query](task-service/docs/mechanisms/board-query.md)                         | `task-service`         | mechanism     | How one board page is read in a fixed number of statements.                                           |
| [Projections](task-service/docs/mechanisms/projections.md)                         | `task-service`         | mechanism     | How task-service knows about projects, categories, statuses and users it does not own.                |
| [template-service](template-service/README.md)                                     | `template-service`     | module README | The reference service to clone when adding a bounded context.                                         |
| [translation-service](translation-service/README.md)                               | `translation-service`  | module README | The translation catalog: keys, languages and their text.                                              |

## Shared libraries and contracts

| Document                                                                                                                     | Module                      | Kind          | Covers                                                                                                   |
|------------------------------------------------------------------------------------------------------------------------------|-----------------------------|---------------|----------------------------------------------------------------------------------------------------------|
| [Avro Contracts](contracts/README.md)                                                                                        | `contracts`                 | module README | The Avro schemas of every event and command, the single source of truth for Kafka payloads.              |
| [shared-archunit](shared-archunit/README.md)                                                                                 | `shared-archunit`           | module README | The architecture rules every service is checked against, as executable tests.                            |
| [shared-messaging-kafka](shared-messaging-kafka/README.md)                                                                   | `shared-messaging-kafka`    | module README | The transactional outbox, idempotent consumption and Avro serialization over Kafka.                      |
| [Events in shared-messaging-kafka](shared-messaging-kafka/src/main/kotlin/com/vertyll/veds/shared/messaging/event/README.md) | `shared-messaging-kafka`    | module README | Why the package has no event marker interfaces or base classes.                                          |
| [shared-saga-api](shared-saga-api/README.md)                                                                                 | `shared-saga-api`           | module README | The saga vocabulary: `Saga`, `SagaStep`, `SagaStatus`, `SagaStepStatus` and `SagaTypeValue`.             |
| [shared-saga-engine](shared-saga-engine/README.md)                                                                           | `shared-saga-engine`        | module README | One service's local saga: the state machine, compensation, the watchdog and the JPA flavor of the ports. |
| [shared-translation-client](shared-translation-client/README.md)                                                             | `shared-translation-client` | module README | Registers a service's translation keys with `translation-service` at start-up.                           |
| [shared-translation](shared-translation/README.md)                                                                           | `shared-translation`        | module README | The key-declaration DSL and the ICU renderer, shared by every service.                                   |
| [shared-web](shared-web/README.md)                                                                                           | `shared-web`                | module README | Keycloak authentication, HTTP concurrency helpers and the shared configuration defaults.                 |

## Infrastructure and scripts

| Document                                                     | Module    | Kind          | Covers                                                                         |
|--------------------------------------------------------------|-----------|---------------|--------------------------------------------------------------------------------|
| [Garage (object storage)](infra/garage/README.md)            | `infra`   | module README | S3-compatible storage for `file-service`.                                      |
| [Kafka Topics IaC (OpenTofu)](infra/kafka/README.md)         | `infra`   | module README | Every Kafka topic and dead letter topic, provisioned with OpenTofu.            |
| [Schema Registry Scripts](scripts/schema_registry/README.md) | `scripts` | module README | Registering the Avro schemas with Schema Registry, and the compatibility mode. |
