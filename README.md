<p align="center">
    <img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
    <img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Apache_Avro-30638E?style=for-the-badge&logo=apacheavro&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/OpenTofu-FFDA18?style=for-the-badge&logo=opentofu&logoColor=black">
    <img alt="" src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
</p>

## Project Assumptions

Microservices back-end for managing projects and their tasks, built on:

- Domain-Driven Design.
- Event-Driven Architecture.
- Hexagonal Architecture.
- Choreography and the Saga pattern for distributed transactions.
- Outbox and inbox patterns for reliable messaging.
- SOLID.

The front-end is [FastDo](https://github.com/vertyll/fastdo).

## Link: https://fastdo.vertyll.dev

## Architecture Graph

![Architecture graph](https://raw.githubusercontent.com/vertyll/veds/refs/heads/main/screenshots/veds-architecture-graph.png)

## Technology Stack

### Back-end:

- Spring Boot.
- Kotlin.
- Gradle Kotlin DSL (a separate build for each service, joined as a composite build).
- PostgreSQL (a separate database for each service).
- Apache Kafka (KRaft).
- Apache Avro with Schema Registry.
- Redis (API gateway sessions).
- Garage (S3-compatible storage for files).
- JUnit.
- Testcontainers.
- Spring Security.
- Spring Data JPA.
- Spring Cloud Gateway.
- OpenAPI (Swagger).

### Authentication:

- **Identity provider**: Keycloak (realm `veds`); the application never sees a password.
- **Pattern**: BFF; only the API gateway holds tokens, the browser holds only a session cookie.
- **Session store**: Redis (Spring Session), every attribute encrypted by the gateway.
- **JWT**: exchanged per service (RFC 8693), so a token leaked from one service opens no other.
- **Authorization**: permissions are granted to roles; each service decides from its own projection.
- **Details**: [Keycloak](docs/keycloak.md) and [Authorization](docs/authorization.md).

### Core back-end:

- The application has an exception handling mechanism (RFC 9457 problem details with message codes).
- The application has a logging mechanism.
- The application has separate environments for local and prod.
- The application has a dedicated configuration file.
- The application has RBAC with permissions per role, announced to every service through Kafka.
- The application has Flyway database migration mechanism.
- The application has optimistic locking with ETag and If-Match headers.
- The application has translations (ICU MessageFormat) owned by the service that declares them.
- And many other features that can be found in the application code.

### Other:

- Docker for development environment.
- Detekt for static code analysis.
- ktlint for code formatting.
- ArchUnit for executable architecture rules.
- Dokka for code documentation.
- OpenTofu for Kafka topic provisioning.

## Documentation

- [Contents](CONTENTS.md) – every document in the repository, the module it belongs to, and what it covers.
- [Glossary](GLOSSARY.md) – every term the docs use, and where it is explained.
- [Standards](STANDARDS.md) – the RFCs and specifications the code implements or depends on.
