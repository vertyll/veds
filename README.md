<p align="center">
    <img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
    <img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Apache_Avro-30638E?style=for-the-badge&logo=apacheavro&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/OpenTofu-FFDA18?style=for-the-badge&logo=opentofu&logoColor=black">
</p>

## Project Assumptions

A microservices-based architecture following principles:
- Domain-Driven Design.
- Event-Driven Architecture.
- Hexagonal Architecture.
- Separation of Concerns.
- Choreography pattern for service coordination.
- Saga pattern for distributed transactions.
- Outbox pattern for reliable event publishing.
- SOLID.

## Architecture Graph

![Architecture graph](https://raw.githubusercontent.com/vertyll/veds/refs/heads/main/screenshots/veds-architecture-graph.png)

## Technology Stack

- **Back-end**: Spring Boot, Kotlin, Gradle Kotlin DSL (a separate build for each service).
- **Database**: PostgreSQL (a separate instance for each service).
- **Message Broker**: Apache Kafka KRaft (Zookeeper-less).
- **Identity Provider**: Keycloak (OAuth2 / OpenID Connect).
- **API Documentation**: OpenAPI (Swagger).
- **Containerization**: Docker / Podman.
- **Authentication**: Keycloak JWT + refresh tokens (HttpOnly secure cookie via BFF pattern).
- **Testing**: JUnit, Testcontainers.
- **Static Analysis**: ktlint, Detekt.
- **Documentation**: Dokka for code docs.
- **Infrastructure as Code**: OpenTofu for Kafka topic provisioning.
- **Build and Dependency Management**: Gradle with composite builds for modularization.
- **Schema Management**: Apache Avro with Schema Registry for versioning and compatibility.

## Documentation

**New here?** Read in this order: [Development Setup](./docs/development-setup.md) to get it running,
[Architecture](./docs/architecture.md) for the shape of the system, then
[Hexagonal Layering](./docs/hexagonal-layering.md) and [Shared Modules](./docs/shared-modules.md) — between them, they
explain nearly every "why is this in a separate module?" question the code raises.

**Getting it running**

- [Development Setup](./docs/development-setup.md) — running the whole system locally, from a fresh clone.
- [Testing](./docs/testing.md) — the two test tiers and the architecture check.

**How the code is organized**

- [Architecture](./docs/architecture.md) — components and design principles.
- [Hexagonal Layering](./docs/hexagonal-layering.md) — the dependency rule, how it is enforced, and what it cost.
- [Shared Modules](./docs/shared-modules.md) — what each shared library is responsible for, and the two rules that decide when a new one is justified.
- [Service Dependencies](./docs/service-dependencies.md) — what each service needs to build and to run.
- [CQRS](./docs/cqrs.md) — where command/query separation is applied, where it is not, and why.

**How services talk to each other**

- [Eventual Consistency](./docs/eventual-consistency.md) — outbox, inbox and sagas: how services agree on state without a distributed transaction.
- [Event Catalogue](./docs/events.md) — every topic, its owner and its consumers.
- [Concurrency Control](./docs/concurrency.md) — optimistic locking, ETags, saga and outbox concurrency.

**Cross-cutting features**

- [Keycloak Configuration](./docs/keycloak.md) — realm setup, authentication flow, role management.
- [Files](./docs/files.md) — pre-signed uploads, a private bucket, and the two sweeps.
- [Translations](./docs/translations.md) — key ownership, ICU, and why a missing key renders as the key.

### Per module

Each service documents its own decisions — what it owns, what it deliberately does not, and the
reasoning behind the parts that look surprising.

| Module                                                             |                                                         |
|--------------------------------------------------------------------|---------------------------------------------------------|
| [api-gateway](./api-gateway/README.md)                             | Token Handler, session encryption, filter ordering      |
| [iam-service](./iam-service/README.md)                             | users, roles, permissions                               |
| [mail-service](./mail-service/README.md)                           | delivery, and why failure is an event                   |
| [project-service](./project-service/README.md)                     | projects, membership, the access policy                 |
| [task-service](./task-service/README.md)                           | tasks, the board query, local projections               |
| [notification-service](./notification-service/README.md)           | delivery rules, STOMP transport                         |
| [translation-service](./translation-service/README.md)             | key ownership, ICU, the two-column rule                 |
| [file-service](./file-service/README.md)                           | pre-signed uploads, the two sweeps                      |
| [template-service](./template-service/README.md)                   | the reference service, and what to strip after cloning  |
| [shared-saga-api](./shared-saga-api/README.md)                     | saga vocabulary, framework-free                         |
| [shared-translation](./shared-translation/README.md)               | the key DSL and the ICU renderer                        |
| [shared-web](./shared-web/README.md)                               | Keycloak converters, ETag and optimistic locking        |
| [shared-messaging-kafka](./shared-messaging-kafka/README.md)       | outbox, idempotent consumption, Avro                    |
| [shared-saga-engine](./shared-saga-engine/README.md)               | saga orchestration, compensation, watchdog              |
| [shared-translation-client](./shared-translation-client/README.md) | start-up registration of translation keys               |
| [shared-archunit](./shared-archunit/README.md)                     | executable architecture rules, applied by every service |

Infrastructure directories document themselves too: [contracts](./contracts/README.md),
[infra/kafka](./infra/kafka/README.md), [infra/garage](./infra/garage/README.md).
