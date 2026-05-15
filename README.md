<p align="center">
<img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
<img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
</p>

## Project Assumptions

A microservices-based architecture, following Domain-Driven Design principles, Hexagonal Architecture, Separation of Concerns, SOLID principles, Choreography pattern for service coordination with the Saga pattern for distributed transactions.

## Architecture

![Architecture graph](https://raw.githubusercontent.com/vertyll/veds/refs/heads/main/screenshots/veds-architecture-graph.png)

The project is split into the following components:
1. **API Gateway** – Entry point for all client requests, handles routing to appropriate services, JWT validation, and BFF (Backend-For-Frontend) auth proxy to Keycloak.
2. **IAM Service** – Consolidates user management, roles/permissions, and account operations. Authentication is delegated to Keycloak.
3. **Mail Service** – Handles email sending operations and templates.
4. **Shared Infrastructure** – Shared infrastructure, contracts, and utilities used across all microservices.
5. **Template Service** – Baseline configuration for future microservices.

Each microservice follows Hexagonal Architecture principles with a three-layer structure and has its own PostgreSQL database. Services communicate with each other asynchronously via Apache Kafka (event-driven, choreography-based), with the Transactional Outbox pattern guaranteeing reliable event publication.

### Detailed Description of Components

#### Kafka KRaft Mode
- The system uses Kafka in KRaft mode (Kafka Raft), eliminating the need for Zookeeper.
- Configuration is handled via `KAFKA_PROCESS_ROLES` (broker, controller) and `KAFKA_CONTROLLER_QUORUM_VOTERS`.
- A static `CLUSTER_ID` is provided in `docker-compose.yml` for simplified setup.

#### Shared Infrastructure
- Provides shared code, DTOs, event definitions, and utilities for all microservices.
- Implements contracts for shared patterns like an Outbox pattern.
- Contains reusable components for Kafka integration.
- Ensures consistency in how services communicate and process events.
- Includes base classes for implementing event choreography.
- Provides shared ports and adapters interfaces for consistent hexagonal architecture implementation.
- Integration events definitions for inter-service communication.

#### IAM Service
- Responsible for user profile management, authorization, and account operations.
- Authentication is fully delegated to Keycloak (via the `IdentityProviderPort` outbound port, implemented by `KeycloakIdentityProviderAdapter`).
- Uses Keycloak Admin API (via service account) for user provisioning and role sync.
- Database stores:
    - User profile data (first name, last name, preferences) linked by `keycloakId`.
    - Role definitions and user-role assignments.
    - User-specific permissions.
    - Verification tokens.
    - Local saga state (`saga`, `saga_step` tables) for end-to-end traceability of distributed flows.
- Provides endpoints for registration, profile management, and role administration.
- Publishes events to Kafka through the `AuthEventPublisherPort` outbound port (e.g., `MailRequestEvent`).
- Consumes feedback events from mail-service (`MailSentEvent` / `MailFailedEvent`) and runs compensation logic via a dedicated `saga-compensation-iam` Kafka topic + `AuthCompensationService` use case (rollback user in Keycloak, delete verification token, revert email/password change).

#### Mail Service
- Responsible for sending emails based on Thymeleaf templates.
- Database stores:
    - Email logs and delivery status (`email_log` table).
    - Local saga state (`saga`, `saga_step` tables).
- Consumes `MailRequestEvent` events from other services (`MailEventConsumer`).
- Publishes `MailSentEvent` / `MailFailedEvent` back through the Transactional Outbox (`KafkaOutboxProcessor`).
- Supports various email templates (welcome, password reset, account activation, email change confirmation, etc.).
- Acts as a reactive participant in the choreography flow — has no outbound business-event port (publication is a saga mechanic, not a domain concern).
- Uses hexagonal architecture to decouple email sending logic from the SMTP/mail provider.

#### Template Service
- A baseline skeleton for spinning up a new microservice — structurally 1:1 with `mail-service`.
- Provides:
    - Hexagonal package layout (`domain/model`, `domain/repository`, `application/service`, `application/saga/{model,port}`, `application/port/out`, `infrastructure/{persistence,kafka,saga,web,config,response,exception}`).
    - Placeholder domain (`Template`, `TemplateStatus`, `TemplateRepository`) with rich-domain methods (`markProcessed`, `markFailed`).
    - Transactional Outbox scaffolding: local JPA entity implementing the shared read-only `OutboxMessage` contract, ensuring guaranteed decoupled event publication.
    - Saga choreography scaffolding: local `saga`/`saga_step` model, `SagaProcessPort`, thin `SagaManagerAdapter` delegating to a shared `SagaEngine` (composition over inheritance, see Saga Pattern section), neutral compensation topic `saga-compensation-template`.
    - Kafka skeleton: outbound `TemplateEventPublisherPort` + `KafkaTemplateEventPublisherAdapter` (using `KafkaOutboxProcessor`), inbound `TemplateEventConsumer`.
    - REST controller `/template` + DTOs.
- Intended to be copied and renamed when starting a new service.

## Technology Stack

- **Back-end**: Spring Boot, Kotlin, Gradle Kotlin DSL.
- **Database**: PostgreSQL (a separate instance for each service).
- **Message Broker**: Apache Kafka KRaft (Zookeeper-less).
- **Identity Provider**: Keycloak (OAuth2 / OpenID Connect).
- **API Documentation**: OpenAPI (Swagger).
- **Containerization**: Docker/Podman containers.
- **Authentication**: Keycloak JWT + refresh tokens (HttpOnly secure cookie via BFF pattern).
- **Testing**: JUnit, Testcontainers.

## Development Setup

### Prerequisites

- Docker/Podman
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
- `template-service` (skeleton — not started by default)
- `shared-infrastructure` (library)

4. Access the services:
- API Gateway: http://localhost:8080
- IAM Service: http://localhost:8082
- Mail Service: http://localhost:8083
- Template Service: http://localhost:8084 (when started; baseline skeleton)
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
- `domain/`: Pure, framework-free core. Rich aggregate models (immutable `data class` with business methods like `User.withRole(...)`, `VerificationToken.markUsed()`, `EmailLog.markAsSent()`), domain enums (e.g. `EmailTemplate`, `TokenTypes`, `EmailStatus`), and **outbound port interfaces** for aggregate repositories (e.g. `UserRepository`, `EmailLogRepository`).
- `application/`: Use cases — application services orchestrating the domain through ports (e.g. `AuthService`, `EmailSagaService`, `RoleService`, `UserService`, `AuthCompensationService`). Also contains:
    - `application/port/out/` — outbound technology-facing ports (e.g. `AuthEventPublisherPort`, `IdentityProviderPort`).
    - `application/saga/model/` — saga aggregates (`Saga`, `SagaStep`, enums `SagaTypes`, `SagaStepNames`, `SagaCompensationActions`).
    - `application/saga/port/` — saga ports (`SagaProcessPort`, `SagaRepository`, `SagaStepRepository`).
- `infrastructure/`: Adapters (both driving and driven).
    - `infrastructure/web/{controller,dto}/` — REST inbound adapter.
    - `infrastructure/persistence/{entity,repository,adapter}/` — JPA inbound and adapter implementations of repository ports (entities suffixed `*JpaEntity`, Spring Data interfaces suffixed `*JpaRepository`).
    - `infrastructure/kafka/` — Kafka inbound consumers and outbound publisher adapters.
    - `infrastructure/saga/` — saga manager and compensation adapters.
    - `infrastructure/security/` — Spring Security and Keycloak adapters.
    - `infrastructure/config/`, `infrastructure/exception/`, `infrastructure/response/` — wiring, error handling, response envelopes.

Implementation details inside `infrastructure/persistence/{entity,repository,adapter}/` and `infrastructure/saga/*Adapter` are declared `internal` to hide adapter internals from the rest of the module.

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

### Saga Pattern & Transactional Outbox (Choreography and Polyglot Persistence)

The system uses **choreography-based sagas** — there is no central orchestrator. Each participating service runs its own local saga and progresses by reacting to domain events on Kafka.

**Polyglot Persistence via Shared Contracts:**
The implementation of both the **Saga** and **Transactional Outbox** patterns relies on extracted, database-agnostic contracts (`OutboxMessage`, `Saga`, `SagaStep`) defined in the `shared-infrastructure` module. 
- **Immutable Domain (`val`)**: These contracts are strictly read-only domain models (using Kotlin `val` properties), enforcing immutability and encapsulating business rules. They are completely decoupled from any database or ORM.
- **Polyglot Persistence**: Because the shared abstractions know nothing about the storage mechanism, every microservice has full architectural freedom to choose its own desired database (e.g., PostgreSQL, MongoDB). Inside its infrastructure adapter, the microservice implements these contracts using its own mapping and mutable DB entities (using `var` as required by frameworks like Hibernate).

Key building blocks:
1. **Transactional Outbox & Saga Engines** (`SagaEngine`, `KafkaOutboxProcessor` from `shared-infrastructure`): Reusable choreography coordinators operating strictly on interfaces. They ensure at-least-once event publication (no dual-write problem) and orchestrate compensation flows.
2. Domain facts on Kafka topics — services react to events, never receive commands.
3. `AWAITING_RESPONSE` saga state — the initiating saga waits for the feedback event correlated by `sagaId` (Saga Log Correlation pattern).
4. Idempotency through localized status checks and unique constraints before re-applying a step.
5. Recovery via scheduled jobs (e.g., `@Scheduled` inside `SchedulingConfig`) that poll the database for stuck sagas based on the provided generic interface implementations.

Example: User Registration (iam-service ⇄ mail-service)
1. `iam-service` (`AuthService.register`) begins a local saga `USER_REGISTRATION`, records steps `CREATE_USER` → `PUBLISH_USER_REGISTERED_EVENT` → `CREATE_VERIFICATION_TOKEN` → `PUBLISH_MAIL_REQUESTED_EVENT`, then transitions to `AWAITING_RESPONSE` and publishes `MailRequestedEvent` through the Outbox.
2. `mail-service` (`MailEventConsumer`) consumes the event, begins its own local saga `EMAIL_SENDING`, performs `SEND_EMAIL`, completes its saga, and publishes `MailSentEvent` (or `MailFailedEvent`) through the Outbox.
3. `iam-service` (`MailFeedbackConsumer`) consumes the feedback:
   - on `MailSentEvent` → `markSagaCompleted`,
   - on `MailFailedEvent` → `markSagaFailed` + publishes compensation actions to the internal `saga-compensation-iam` topic; `SagaCompensationService` listens and delegates to the `AuthCompensationService` use case (rollback Keycloak user, delete verification token, revert email/password change).

Compensation only exists where effects are reversible — `mail-service` therefore does **not** have a `SagaCompensationService` (a sent email cannot be un-sent).

### Event-Driven Communication

Services communicate asynchronously through Kafka events (definitions in `shared-infrastructure`):
- **`MailRequestedEvent`** — published by `iam-service` (via `AuthEventPublisherPort`/`KafkaAuthEventPublisherAdapter`), consumed by `mail-service` (`MailEventConsumer`).
- **`MailSentEvent`** / **`MailFailedEvent`** — published by `mail-service` through the Transactional Outbox, consumed by `iam-service` (`MailFeedbackConsumer`) to advance or fail the originating saga.
- **Saga compensation actions** — published by `iam-service` to its own internal `saga-compensation-iam` topic (e.g. `DELETE_USER`, `DELETE_VERIFICATION_TOKEN`, `REVERT_PASSWORD_UPDATE`, `REVERT_EMAIL_UPDATE`), consumed by `SagaCompensationService` and dispatched to `AuthCompensationService`.

Event publishing and consuming are handled by dedicated adapters in `infrastructure/kafka/`, keeping the domain core focused on business logic.

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
- Template Service: http://localhost:8084/swagger-ui.html (when started)

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
