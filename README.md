<p align="center">
<img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
<img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
</p>

## Project Assumptions

A microservices-based architecture, following Domain-Driven Design (DDD) principles, Hexagonal Architecture (Ports & Adapters), Separation of Concerns (SoC), SOLID principles, Choreography pattern for service coordination, and the Saga pattern for distributed transactions.

## Architecture

![Architecture graph](https://raw.githubusercontent.com/vertyll/veds/refs/heads/main/screenshots/veds-architecture-graph.png)

The project is split into the following components:
1. **API Gateway** – Entry point for all client requests, handles routing to appropriate services, JWT validation, and BFF (Backend-For-Frontend) auth proxy to Keycloak.
2. **IAM Service** – Consolidates user management, roles/permissions, and account operations. Authentication is delegated to Keycloak.
3. **Mail Service** – Handles email sending operations and templates.
4. **Shared Infrastructure** – Shared infrastructure, contracts, and utilities used across all microservices.
5. **Template Service** – Baseline configuration for future microservices.

Each microservice follows Hexagonal Architecture principles with a three-layer structure and has its own PostgreSQL database. Services communicate with each other via Apache Kafka for event-driven architecture, primarily for notifying the Mail Service.

### Detailed Description of Components

#### Kafka KRaft Mode
- The system uses Kafka in KRaft mode (Kafka Raft), eliminating the need for Zookeeper.
- Configuration is handled via `KAFKA_PROCESS_ROLES` (broker, controller) and `KAFKA_CONTROLLER_QUORUM_VOTERS`.
- A static `CLUSTER_ID` is provided in `docker-compose.yml` for simplified setup.

#### Shared Infrastructure
- Provides shared code, DTOs, event definitions, and utilities for all microservices.
- Implements shared patterns like an Outbox pattern.
- Contains reusable components for Kafka integration, exception handling, and API responses.
- Ensures consistency in how services communicate and process events.
- Includes base classes for implementing event choreography.
- Provides shared ports and adapters interfaces for consistent hexagonal architecture implementation.
- Integration events definitions for inter-service communication.

#### IAM Service
- Responsible for user profile management, authorization (roles & permissions), and account operations (activation, password/email change).
- Authentication (credentials, tokens) is fully delegated to Keycloak.
- Uses Keycloak Admin API (via service account) for user provisioning and role sync.
- Database stores:
    - User profile data (first name, last name, preferences) linked by `keycloakId`.
    - Role definitions and user-role assignments.
    - User-specific permissions.
    - Verification tokens (activation, password/email change).
- Provides endpoints for registration, profile management, and role administration.
- Publishes mail request events to trigger email workflows.
- Implements an internal Saga pattern for multi-step operations like user registration.

#### Mail Service
- Responsible for sending emails based on templates.
- Database stores:
    - Email logs.
    - Delivery status.
- Consumes mail request events from other services.
- Supports various email templates (welcome, password reset, account activation, etc.).
- Acts as a reactive service in the choreography flow.
- Uses hexagonal architecture to decouple email sending logic from external mail providers.

#### Template Service
- A template for implementing hexagonal architecture.
- Example domain models, repositories, and services.
- Sample event definitions and Kafka integration.
- Basic Saga pattern implementation.

## Technology Stack

- **Back-end**: Spring Boot, Kotlin, Gradle Kotlin DSL.
- **Database**: PostgreSQL (a separate instance for each service).
- **Message Broker**: Apache Kafka KRaft (Zookeeper-less).
- **Identity Provider**: Keycloak (OAuth2 / OpenID Connect).
- **API Documentation**: OpenAPI (Swagger).
- **Containerization**: Docker/Podman, Docker Compose/Podman Compose.
- **Authentication**: Keycloak JWT + refresh tokens (HttpOnly secure cookie via BFF pattern).
- **Testing**: JUnit, Testcontainers.

## Development Setup

### Prerequisites

- Docker/Podman and Docker Compose/Podman Compose
- JDK 25 LTS
- Gradle

### Getting Started

1. Clone the repository:
```bash
git clone https://github.com/vertyll/veds.git
# and
cd veds
```

2. Start the infrastructure:
```bash
docker-compose up -d
```

3. Build and run microservices:

**Building:**
```bash
cd <service-name>
./gradlew build
```

**Local Running:**
```bash
cd <service-name>
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**Available Services:**
- `api-gateway`
- `iam-service`
- `mail-service`
- `shared-infrastructure` (library)

4. Access the services:
- API Gateway: http://localhost:8080
- IAM Service: http://localhost:8082
- Mail Service: http://localhost:8083
- Keycloak: http://localhost:9000
- Kafka UI: http://localhost:8090
- MailDev: http://localhost:1080

## Keycloak Configuration

### How it works

Keycloak is the identity provider (IdP) for the application. It handles:
- User credentials storage (passwords, enabled/disabled state).
- Token issuance (access tokens + refresh tokens, JWT format).
- Role management (mirrored from the app's IAM service).

**The realm JSON export (`keycloak/realm-config/realm-export.json`) is automatically imported on first startup** via Docker Compose volume mount. You do **not** need to configure Keycloak manually.

### What the realm export creates

| Resource                     | Name                   | Purpose                                                                       |
|------------------------------|------------------------|-------------------------------------------------------------------------------|
| Realm                        | `veds`                 | Application realm                                                             |
| Realm roles                  | `USER`, `ADMIN`        | Mapped to Spring Security `ROLE_USER`, `ROLE_ADMIN`                           |
| Client                       | `veds-api-gateway`     | Confidential client used by the Gateway BFF for login/refresh/logout          |
| Client                       | `veds-service-account` | Service account for IAM backend admin operations (user CRUD, role assignment) |
| Protocol mapper (predefined) | `roles mapper`         | Puts realm roles into `realm_access.roles` claim in access token              |
| Protocol mapper (predefined) | `email mapper`         | Puts `email` claim in access token                                            |

### Authentication flow

```
Frontend ──► API Gateway (BFF) ──► Keycloak
                │
                ├── POST /auth/token          → login, returns accessToken in body + refreshToken in HttpOnly cookie
                ├── POST /auth/refresh-token  → reads cookie, refreshes tokens
                └── POST /auth/logout         → invalidates token, clears cookie

Frontend ──► API Gateway ──► Microservices (Bearer token)
                │
                └── Authorization: Bearer <accessToken>
                    Each microservice validates JWT independently via Keycloak's JWKS endpoint
```

### Configuration in `shared-config.yml`

All Keycloak-related config is centralized in `shared-infrastructure/src/main/resources/shared-config.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:9000/realms/veds}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:9000/realms/veds/protocol/openid-connect/certs}

veds:
  shared:
    keycloak:
      server-url: ${KEYCLOAK_SERVER_URL:http://localhost:9000}
      realm: ${KEYCLOAK_REALM:veds}
      admin-client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:veds-service-account}
      admin-client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:KEYCLOAK_ADMIN_CLIENT_SECRET_HERE}
      gateway-client-id: ${KEYCLOAK_GATEWAY_CLIENT_ID:veds-api-gateway}
      gateway-client-secret: ${KEYCLOAK_GATEWAY_CLIENT_SECRET:KEYCLOAK_GATEWAY_CLIENT_SECRET_HERE}
      roles-claim-path: realm_access.roles
      cookie:
        refresh-token-cookie-name: KEYCLOAK_REFRESH_TOKEN
        http-only: true
        secure: ${COOKIE_SECURE:false}
        same-site: Strict
        path: "/"
```

### Useful Keycloak URLs (local dev)

| URL                                                                | Description                             |
|--------------------------------------------------------------------|-----------------------------------------|
| http://localhost:9000                                              | Keycloak admin console                  |
| http://localhost:9000/realms/veds/.well-known/openid-configuration | OpenID Connect discovery                |
| http://localhost:9000/realms/veds/protocol/openid-connect/certs    | JWKS (public keys for JWT verification) |
| http://localhost:9000/realms/veds/protocol/openid-connect/token    | Token endpoint                          |

### Manual token request (curl)

```bash
# Get access token
curl -s -X POST http://localhost:9000/realms/veds/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=veds-api-gateway" \
  -d "client_secret=KEYCLOAK_GATEWAY_CLIENT_SECRET_HERE" \
  -d "username=test@example.com" \
  -d "password=Test1234!" | jq .
```

## API Testing (Insomnia)

An Insomnia collection is provided at `insomnia-collection.yaml` in the project root.

## Global Management (Convenience)

For convenience, you can use the provided `Makefile` in the root directory:

- Build all services: `make build-all`.
- Run tests in all services: `make test-all`.
- Clean all services: `make clean-all`.

## Architecture Design

### Hexagonal Architecture (Ports & Adapters)

Each microservice implements Hexagonal Architecture to achieve clean separation of concerns.

Benefits:
- **Testability**: Core business logic can be tested independently.
- **Flexibility**: Easy to swap external dependencies without affecting business logic.
- **Maintainability**: Clear separation between business rules and technical details.
- **Technology Independence**: Core domain is not coupled to specific frameworks or technologies.

### Domain-Driven Design (DDD)

Each microservice is designed around a specific business domain with:
- A clear bounded context.
- Domain models that represent business entities.
- A layered architecture within the hexagonal structure.
- Domain-specific language.
- Encapsulated business logic.

The project structure follows DDD principles within hexagonal architecture:
- `domain`: Core business models, domain services, domain events, and business logic.
- `application`: Controllers, DTOs, use cases, application services, and port definitions (Primary Adapters).
- `infrastructure`: Adapters for external systems like databases, Kafka, email providers, etc. (Secondary Adapters).

### SOLID Principles and Separation of Concerns

The codebase adheres to:
- **Single Responsibility Principle**: Each class has a single responsibility.
- **Open/Closed Principle**: Classes are open for extension but closed for modification.
- **Liskov Substitution Principle**: Subtypes are substitutable for their base types.
- **Interface Segregation Principle**: Specific interfaces rather than general ones.
- **Dependency Inversion Principle**: Depends on abstractions (ports), not concretions (adapters).

The hexagonal architecture naturally enforces these principles by:
- Isolating business logic from external concerns.
- Using dependency inversion through ports and adapters.
- Maintaining clear boundaries between layers.

### Choreography Pattern for Service Coordination

The system uses a choreography-based approach for service coordination:
- Services react to events published by other services without central coordination.
- Each service knows which events to listen for and what actions to take.
- No central orchestrator is needed, making the system more decentralized and resilient.
- Services maintain autonomy and can evolve independently.

Benefits of this approach:
- Reduced coupling between services.
- More flexible and scalable architecture.
- Easier to add new services or modify existing ones.
- Better resilience as there's no single point of failure.
- Aligns well with hexagonal architecture by treating event communication as external adapters.

### Saga Pattern for Distributed Transactions

For distributed transactions, we use the Saga pattern (currently internal to IAM Service but can be extended):
1. A service or component initiates a multistep operation (e.g., registration).
2. Each step is recorded in the Saga state machine.
3. If a step fails, compensating transactions are triggered to maintain consistency.

Example: User Registration Saga (Internal to IAM Service)
- Create User: Creates the user record.
- Create Verification Token: Generates a token for account activation.
- Send Welcome Email: Publishes a `MailRequestedEvent` to Kafka for the Mail Service.

Each step is handled through hexagonal architecture, ensuring clean separation between the domain logic and persistence/event adapters.

### Event-Driven Communication

Services communicate asynchronously through Kafka events:
- **UserActivatedEvent**: Triggered when a user activates their account.
- **MailRequestedEvent**: Triggered when an email needs to be sent (consumed by Mail Service).
- **SagaCompensationEvent**: Internal event for triggering compensation logic.

Event publishing and consuming are handled by dedicated adapters, keeping the domain core focused on business logic.

## Optimistic Locking and Concurrency Control

This project uses a combination of HTTP ETags (at API boundaries) and JPA Optimistic Locking (within services) to prevent lost updates and to ensure safe concurrency across microservices and asynchronous processing.

### Summary
- API layer: ETag/If-Match for conditional updates from clients (front-end, API consumers).
- Persistence layer: JPA `@Version` on entities and the load → mutate → save a pattern inside a single transaction.
- Sagas and Outbox: Internal consistency ensured by JPA Optimistic Locking and idempotency safeguards — no HTTP ETags here.

### Error semantics at the API layer:
- `428 Precondition Required` — missing `If-Match` on required endpoints.
- `412 Precondition Failed` — ETag/If-Match does not match the current version.
- `409 Conflict` — last-resort handler for JPA `ObjectOptimisticLockingFailureException` (race detected at commit time).

### Persistence: JPA Optimistic Locking
- Entities use `@Version` to enable Optimistic Locking (e.g., `User`, `Role`, `KafkaOutbox`, and Saga/SagaStep entities where applicable).
- Services follow the pattern: load the entity → apply changes → `save(...)` inside `@Transactional`. Hibernate includes `WHERE version = ?` and raises a conflict if data changed concurrently.

### Sagas (Internal, no ETag)
- Sagas are backend-internal processes (event-driven), not HTTP resources — therefore **ETag/If-Match is not used in Sagas**.
- Concurrency control:
    - `@Version` on Saga and/or SagaStep where applicable, persisted via a load → mutate → save.
    - Idempotency for steps:
        - Database-level unique constraint on `(sagaId, stepName)` prevents duplicate step insertion.
        - Service-level soft check in `recordSagaStep` returns the existing step if already present (safe retries/duplicates).
- Compensation steps are recorded and published through Outbox when needed.

### Outbox Pattern
- `KafkaOutbox` has `@Version`.
- The `KafkaOutboxProcessor` updates message status by mutating the loaded entity and calling `save(...)` (no bulk JPQL updates). This leverages Optimistic Locking to avoid races between processor instances.
- On failure, the processor increments `retryCount`, stores the error, and persists via `save(...)` again.

## API Documentation

Each service provides its own Swagger UI for API documentation:
- IAM Service: http://localhost:8082/swagger-ui.html
- Mail Service: http://localhost:8083/swagger-ui.html

## Monitoring

- Each service exposes health and metrics endpoints through Spring Boot Actuator
- Health checks can be accessed at `/actuator/health` on each service
- Metrics can be collected for observability and monitoring service health

## Formatting and code style

The project uses ktlint for code formatting and style checks. Go to the service directory and run:

```bash
./gradlew ktlintFormat
```

To check the code style, run:

```bash
./gradlew ktlintCheck
```

You can also use `make format-all` and `make check-style-all` from the root directory.
