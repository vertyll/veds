<p align="center">
    <img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
    <img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Apache_Avro-30638E?style=for-the-badge&logo=apacheavro&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Terraform-844FBA?style=for-the-badge&logo=terraform&logoColor=white">
</p>

## Project Assumptions

A microservices-based architecture following principles:
- Domain-Driven Design.
- Hexagonal Architecture.
- Separation of Concerns.
- Choreography pattern for service coordination.
- Saga pattern for distributed transactions.
- Outbox pattern for reliable event publishing.
- SOLID.

![Architecture graph](https://raw.githubusercontent.com/vertyll/veds/refs/heads/main/screenshots/veds-architecture-graph.png)

## Technology Stack

- **Back-end**: Spring Boot, Kotlin, Gradle Kotlin DSL.
- **Database**: PostgreSQL (a separate instance for each service).
- **Message Broker**: Apache Kafka KRaft (Zookeeper-less).
- **Identity Provider**: Keycloak (OAuth2 / OpenID Connect).
- **API Documentation**: OpenAPI (Swagger).
- **Containerization**: Docker / Podman.
- **Authentication**: Keycloak JWT + refresh tokens (HttpOnly secure cookie via BFF pattern).
- **Testing**: JUnit, Testcontainers.
- **Static Analysis**: ktlint, Detekt.
- **Documentation**: Dokka for code docs.
- **Infrastructure as Code**: Terraform for Kafka topic provisioning.
- **Build and Dependency Management**: Gradle with composite builds for modularization.
- **Schema Management**: Apache Avro with Schema Registry for versioning and compatibility.

## Documentation

- [Architecture](./docs/architecture.md) — components, design principles, hexagonal architecture, DDD, choreography pattern.
- [Saga Pattern & Transactional Outbox](./docs/saga-and-outbox.md) — saga engine, outbox, idempotent receiver, event-driven communication.
- [Concurrency Control](./docs/concurrency.md) — optimistic locking, ETags, saga and outbox concurrency.
- [Keycloak Configuration](./docs/keycloak.md) — realm setup, authentication flow, role management.
- [Development Setup](./docs/development-setup.md) — prerequisites, getting started, service URLs, API testing, code style.
