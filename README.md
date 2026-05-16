r
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
5. **`iam-contracts` / `mail-contracts` / `template-contracts`** – Per-bounded-context Avro Published Language modules (DDD). Each holds only Avro `*.avsc` schemas and the Java SpecificRecord classes generated from them; consumed as a regular library JAR by the producing/consuming services. See *Avro Published Language modules* below.
6. **Template Service** – Baseline configuration for future microservices.

Each microservice follows Hexagonal Architecture principles with a three-layer structure and has its own PostgreSQL database. Services communicate with each other asynchronously via Apache Kafka (event-driven, choreography-based), with the Transactional Outbox pattern guaranteeing reliable event publication.

### Detailed Description of Components

#### Kafka KRaft Mode
- The system uses Kafka in KRaft mode (Kafka Raft), eliminating the need for Zookeeper.
- Configuration is handled via `KAFKA_PROCESS_ROLES` (broker, controller) and `KAFKA_CONTROLLER_QUORUM_VOTERS`.
- A static `CLUSTER_ID` is provided in `docker-compose.yml` for simplified setup.

#### Shared Infrastructure
- Provides shared code, DTOs, event definitions, and utilities for all microservices.
- Implements **persistence-agnostic contracts** for the Transactional Outbox and Saga patterns (`OutboxRepositoryPort`, `ProcessedEventRepositoryPort`, `SagaRepositoryPort`, `SagaStepRepositoryPort`) — any database (PostgreSQL, MongoDB, …) can plug in by implementing the ports.
- Reusable Kafka building blocks: `KafkaOutboxProcessor` + `OutboxDispatchTx` (two-phase claim/dispatch), `ProcessedEventGuard` (idempotent receiver), `AvroPayloadSerializer`/`AvroPayloadDeserializer`.
- Shared **technical IdP adapter** for OAuth2/OIDC: `KeycloakJwtAuthenticationConverter` (servlet) and `ReactiveKeycloakJwtAuthenticationConverter` (WebFlux) — wired via `KeycloakSecurityAutoConfiguration`. They translate the configured roles claim (`veds.shared.keycloak.roles-claim-path`) into Spring Security `ROLE_*` authorities without knowing any concrete role names — so they are role-vocabulary-agnostic and remain in `shared-infrastructure` as a pure technical adapter (see *Where do role names live?* below).
- Saga engine: generic `SagaEngine`, `SagaCompensationRunner`, `SagaWatchdog` operating on F-bounded contracts (`Saga<S : Saga<S>>`, `SagaStep<T : SagaStep<T>>`) — zero unchecked casts in the engine.
- Externalized configuration: `KafkaOutboxProperties` (`veds.outbox.*`) and `SagaProperties` (`veds.saga.*`) auto-registered by `OutboxAndSagaAutoConfiguration`.
- Composable contracts for inter-service conventions: `SagaCompensationTopic.PREFIX` (each service composes its own `saga-compensation-<participant>` topic).
- Integration event schemas defined as Avro (`contracts/<service>/<topic>/v<n>/*.avsc`) — binary wire format with Schema Registry; Jackson is no longer used on the wire (kept only for saga payload JSON in DB and API Gateway HTTP).

#### Avro Published Language modules (`iam-contracts`, `mail-contracts`, `template-contracts`)

- **Why a separate module per bounded context (DDD *Published Language*):**
  Avro schemas owned by a service end up as generated `SpecificRecord` Java classes. Generating them inside the consuming service produced a classic Spring Boot DevTools pitfall: the generated classes were loaded by `RestartClassLoader` while collaborator code in `shared-infrastructure` used the application classloader, yielding `ClassCastException: X cannot be cast to X` at runtime.
  Lifting the contracts into their own composite-build modules (`iam-contracts/`, `mail-contracts/`, `template-contracts/`) ships them as plain library JARs, so they always resolve via the application classloader — the issue cannot recur even when DevTools is added.
- **What each module contains:** a single `java-library` Gradle build that runs `avro-tools` over the `*.avsc` files and produces a SpecificRecord JAR (`api("org.apache.avro:avro:...")` so consumers get the runtime transitively).
- **Where the schemas live:**
    - `iam-contracts` / `mail-contracts` read schemas from the repository-root `contracts/<service>/**` directory — single source of truth shared with the Terraform topic provisioner and the Schema Registry registration script.
    - `template-contracts` keeps its schemas **locally** (`template-contracts/avro/**`) — same rationale as before: the template service is intentionally excluded from production schema/topic provisioning. When cloning, move the schemas under `contracts/<new-service>/`.
- **How services consume them:** `includeBuild("../iam-contracts")` + `implementation("com.vertyll.veds:iam-contracts")` in each service `settings.gradle.kts` / `infrastructure/build.gradle.kts`.
- **Anti-Corruption Layer (ACL):** generated Avro types **never** leave the `infrastructure/saga/` package. A dedicated translator (`AvroAuthCompensationCommandTranslator`, `AvroTemplateCompensationCommandTranslator`) decodes raw bytes into a Kotlin `sealed interface` (`AuthCompensationCommand`, `TemplateCompensationCommand`) living in `application/saga/model/`. The application layer therefore stays Avro-, Jackson-, Kafka- and Spring-free, and compensation dispatch is an exhaustive `when` over a typed hierarchy (no `Map<String, Any?>`, no stringly-typed `action` discriminators, compile-time-checked).
- **DevTools:** intentionally NOT declared in `iam-service/infrastructure`, `mail-service/infrastructure`, `template-service/infrastructure` — see the comments in their `build.gradle.kts`.

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
- Publishes events to Kafka through the `AuthEventPublisherPort` outbound port (e.g., `MailRequestedEvent`).
- Consumes feedback events from mail-service (`MailSentEvent` / `MailFailedEvent`) and runs compensation logic via a dedicated `saga-compensation-iam` Kafka topic + `AuthCompensationService` use case (rollback user in Keycloak, delete verification token, revert email/password change). The compensation envelope is an Avro **tagged union** (`DeleteUserAction | DeleteVerificationTokenAction | RevertPasswordUpdateAction | RevertEmailUpdateAction`) decoded by an ACL translator into the application-layer `sealed interface AuthCompensationCommand` — see *Avro Published Language modules* above.

#### Mail Service
- Responsible for sending emails based on Thymeleaf templates.
- Database stores:
    - Email logs and delivery status (`email_log` table).
    - Local saga state (`saga`, `saga_step` tables).
- Consumes `MailRequestedEvent` events from other services (`MailEventConsumer`).
- Publishes `MailSentEvent` / `MailFailedEvent` back through the Transactional Outbox (`KafkaOutboxProcessor`).
- Supports various email templates (welcome, password reset, account activation, email change confirmation, etc.).
- Acts as a reactive participant in the choreography flow — has no outbound business-event port (publication is a saga mechanic, not a domain concern).
- Uses hexagonal architecture to decouple email sending logic from the SMTP/mail provider.

#### Template Service
- A baseline skeleton for spinning up a new microservice — structurally 1:1 with `mail-service`.
- **Excluded from the root composite build** (`settings.gradle.kts`), from CI/CD pipelines, and from the global `contracts/` directory. Its Avro schemas live locally in a paired Published Language module `template-contracts/avro/` so they are **not** picked up by the production provisioner image or Terraform topic provisioning.
- Compile-only: `cd template-service && ./gradlew build` (the `includeBuild("../template-contracts")` line in `template-service/settings.gradle.kts` pulls in the contracts jar automatically).
- Provides:
    - Hexagonal package layout (`domain/model`, `domain/repository`, `application/service`, `application/saga/{model,port}`, `application/port/{inbound,out}`, `infrastructure/{persistence,kafka,saga,web,config,response,exception}`).
    - Placeholder domain (`Template`, `TemplateStatus`, `TemplateRepository`) with rich-domain methods (`markProcessed`, `markFailed`).
    - Transactional Outbox scaffolding: local JPA entity implementing the shared read-only `OutboxMessage` contract, ensuring guaranteed decoupled event publication.
    - Saga choreography scaffolding: local `saga`/`saga_step` model, `SagaProcessPort`, thin `SagaManagerAdapter` delegating to a shared `SagaEngine` (composition over inheritance, see Saga Pattern section), neutral compensation topic `saga-compensation-template`.
    - **Typed compensation pipeline** mirroring `iam-service`: sealed `TemplateCompensationCommand` in the application layer, `AvroTemplateCompensationCommandTranslator` (ACL) in infrastructure, `TemplateCompensationEventSerializer`, `TemplateSagaCompensator`, `TemplateSagaCompensationHandler`, `SagaCompensationService` (Kafka inbound) and a placeholder `TemplateCompensationService` use case to wire from on clone.
    - Kafka skeleton: outbound `TemplateEventPublisherPort` + `KafkaTemplateEventPublisherAdapter` (using `KafkaOutboxProcessor`), inbound `TemplateEventConsumer`.
    - REST controller `/template` + DTOs.
- **When cloning**: copy `template-service/` **and** `template-contracts/`, rename packages and module names, **move `template-contracts/avro/*.avsc` into the shared `contracts/<new-service>/`** so the new service participates in global schema registration and topic provisioning, point its `template-contracts/build.gradle.kts` at the new location, and add `includeBuild("<new-service>")` + `includeBuild("<new-service>-contracts")` lines to the root `settings.gradle.kts`.

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
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Available Services:**
- `api-gateway`
- `iam-service`
- `mail-service`
- `template-service` (reference template — excluded from root composite build; compile standalone only)
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

### Where do role names live? (microservices anti–shared-kernel)

Role names (`USER`, `ADMIN`) are owned by **two places only**:
1. **Keycloak realm** (`keycloak/realm-config/realm-export.json`) — the runtime source of truth issued in every access token's `realm_access.roles` claim.
2. **iam-service** (`iam-service/.../domain/model/RoleType.kt`, `internal`) — a type-safe mirror used solely by the role *administrator* (`RoleInitializer` seeds the DB, `AuthService.register` assigns `USER` via the Keycloak Admin API). The enum is `internal` to the iam-service module and intentionally **not** exported.

Other microservices (`api-gateway`, `mail-service`, `template-service`) **do not** depend on iam-service's enum. They check roles as plain strings:

```kotlin
// mail-service / template-service / api-gateway SecurityConfig
.requestMatchers("/mail/**").hasRole("ADMIN")

// IAM controllers
@PreAuthorize("hasRole('ADMIN')")
```

Why no `shared-infrastructure.RoleType` enum:
- It would be a **Shared Kernel** (DDD anti-pattern, Evans, *DDD* ch. 14) — every change to the role vocabulary would force a coordinated recompile/deploy across services.
- It would conflict with the **Bounded Context** boundary: a role's *meaning* (what `ADMIN` is allowed to do) belongs to the service that owns the resource, not to a global enum.
- The IdP is already the source of truth; an in-code mirror would inevitably drift from Keycloak.

What stays in `shared-infrastructure/security/` is **only** the technical JWT → `Authentication` adapter (`KeycloakJwtAuthenticationConverter` / `ReactiveKeycloakJwtAuthenticationConverter`). It is role-name-agnostic — it maps *whatever* strings sit in the configured claim path onto `ROLE_*` authorities. Each service then decides which of those it cares about, in its own `SecurityConfig`.

### Useful Keycloak URLs (local dev)| URL                                                                | Description                             |
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

All cross-project actions are driven by **Gradle** at the repository root. The
`.run/` folder contains shareable IntelliJ run configurations covering the same
tasks for IDE users.

| Goal                                      | Gradle                                                                                  | IDE run config                                                                          |
|-------------------------------------------|-----------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| Build everything                          | `./gradlew build`                                                                       | —                                                                                       |
| Run all tests                             | `./gradlew test`                                                                        | —                                                                                       |
| Format (ktlint)                           | `./gradlew ktlintFormat`                                                                | **Quality: ktlintFormat (all)**                                                         |
| Lint only (ktlint)                        | `./gradlew ktlintCheck`                                                                 | **Quality: ktlintCheck (all)**                                                          |
| Static analysis only (detekt)             | `./gradlew detekt`                                                                      | **Quality: detekt (all)**                                                               |
| Static analysis (ktlint + detekt)         | `./gradlew check`                                                                       | **Quality: ktlintCheck + detekt (all)**                                                 |
| Generate Dokka docs                       | `./gradlew docs`                                                                        | **Docs: Dokka (shared-infrastructure)**                                                 |
| Start local infra                         | `./gradlew infraUp bootstrap`                                                           | **Infra: Up** *(Gradle)* / **Infra: Up (compose)** *(native Docker, IDEA Ultimate)*     |
| Stop local infra                          | `./gradlew infraDown`                                                                   | **Infra: Down** *(Gradle)* / **Infra: Down (compose)** *(native Docker, IDEA Ultimate)* |
| Tail container logs                       | `./gradlew infraLogs`                                                                   | —                                                                                       |
| Provision Kafka topics (Terraform, local) | `./gradlew provisionTopics`                                                             | **Infra: Provision topics**                                                             |
| Register Avro schemas (local)             | `./gradlew registerSchemas`                                                             | **Infra: Register schemas**                                                             |
| Provision **prod** topics + schemas       | `podman compose --profile provision run --rm veds-provisioner` *(on server, see below)* | —                                                                                       |
| Run all services together                 | —                                                                                       | **All services** (Compound)                                                             |

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
    - `application/saga/model/` — saga aggregates (`Saga`, `SagaStep`, enums `SagaTypes`, `SagaStepNames`, and the per-service `*CompensationCommand` sealed interface — e.g. `AuthCompensationCommand`, `TemplateCompensationCommand` — that mirrors the Avro tagged union).
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

### Saga Pattern & Transactional Outbox (Choreography, Polyglot Persistence)

The system uses **choreography-based sagas** — there is no central orchestrator. Each participating service runs its own local saga and progresses by reacting to domain events on Kafka.

**Polyglot Persistence via Shared Contracts.**
Both the **Saga** and **Transactional Outbox** patterns are built on top of database-agnostic ports defined in `shared-infrastructure`:

| Contract | Purpose |
|---|---|
| `Saga<S : Saga<S>>`, `SagaStep<T : SagaStep<T>>` | Rich-aggregate ports with F-bounded generics — behavior methods return the concrete adapter type, eliminating unchecked casts. |
| `SagaRepositoryPort`, `SagaStepRepositoryPort` | Persistence ports for sagas. |
| `OutboxMessage`, `OutboxRepositoryPort` | Outbox aggregate + repository port (with `lockBatchForDispatch` for `SELECT … FOR UPDATE SKIP LOCKED`). |
| `ProcessedEventRepositoryPort` | Idempotent-receiver ledger (UNIQUE `(eventId, consumerGroup)`). |

`shared-infrastructure` ships JPA flavors (`BaseSaga`, `BaseSagaStep`, `BaseSagaRepository`, `BaseSagaStepRepository`, `OutboxJpaEntity`) — to introduce a different storage (MongoDB, DynamoDB, …) you only implement the ports against the new technology; the engines do not change.

**Canonical building blocks (all in `shared-infrastructure`).**

1. **Transactional Outbox** — `KafkaOutboxProcessor` (poller) + `OutboxDispatchTx` (transactional helper):
   - Two-phase dispatch: a short transaction *claims* a batch with `SELECT … FOR UPDATE SKIP LOCKED` and flips rows to `PROCESSING`; Kafka publication happens *outside* of any DB transaction; success/failure is recorded in a fresh `REQUIRES_NEW` transaction (`markCompleted` / `markRetryScheduled` / `markDeadLettered`).
   - Statuses: `READY → PROCESSING → COMPLETED` (happy path) or `→ DEAD_LETTERED` after exhausting retries (`OutboxStatus`).
   - UNIQUE constraint on `event_id` enforces producer-side idempotency.
   - Stuck-message reaper rescues `PROCESSING` rows abandoned by a crashed publisher (configurable threshold).
   - All knobs externalized via `KafkaOutboxProperties` (`veds.outbox.poll-interval | batch-size | max-retries | retry-cooldown | stuck-threshold`).
2. **Idempotent receiver** — `ProcessedEventGuard.claim(eventId, consumerGroup)` writes to a ledger with UNIQUE `(eventId, consumerGroup)`; duplicate deliveries are detected via caught `DataIntegrityViolationException` and short-circuited (Kafka's at-least-once delivery is neutralized at the consumer boundary).
3. **Saga engine** — generic `SagaEngine<S, T>` with choreography semantics:
   - `startSaga` / `recordSagaStep` / `awaitResponse` / `completeSaga` / `failSaga`.
   - On step failure or explicit `failSaga`, an *after-commit* hook (`TransactionSynchronizationManager`) delegates to `SagaCompensationRunner.runCompensation` which opens its own `REQUIRES_NEW` transaction — so compensation only runs if the caller's business transaction actually commits, and runs through Spring's AOP proxy (no self-invocation pitfalls).
4. **Saga watchdog** — `SagaWatchdog` (`@Scheduled`, `veds.saga.watchdog-interval`):
   - Times out sagas stuck in `AWAITING_RESPONSE` longer than `veds.saga.await-response-timeout` (calls `failSaga`).
   - Retries compensation for sagas in `COMPENSATING` / `COMPENSATION_FAILED` whose `updatedAt` is older than `veds.saga.compensation-retry-cooldown`.
5. **Cross-service compensation topic naming** — `SagaCompensationTopic.PREFIX + "<service>"` (each service composes its own neutral topic, e.g. `saga-compensation-iam`).
6. **Saga Log Correlation** — feedback events carry `sagaId`; the originating saga sits in `AWAITING_RESPONSE` until matched.
7. **Recovery via scheduled jobs** — service-local `SchedulingConfig` wires `@EnableScheduling` so `KafkaOutboxProcessor` and `SagaWatchdog` ticks fire.

Example: User Registration (iam-service ⇄ mail-service)
1. `iam-service` (`AuthService.register`) begins a local saga `USER_REGISTRATION`, records steps `CREATE_USER` → `PUBLISH_USER_REGISTERED_EVENT` → `CREATE_VERIFICATION_TOKEN` → `PUBLISH_MAIL_REQUESTED_EVENT`, transitions to `AWAITING_RESPONSE`, and inserts a `MailRequestedEvent` row into the outbox (same JDBC transaction as the user write — no dual-write).
2. The outbox poller publishes the Avro-serialized `MailRequestedEvent` to Kafka.
3. `mail-service` (`MailEventConsumer`) consumes the event, claims it via `ProcessedEventGuard`, begins its own local saga `EMAIL_SENDING`, performs `SEND_EMAIL`, completes its saga, and inserts `MailSentEvent` (or `MailFailedEvent`) into its outbox.
4. `iam-service` (`MailFeedbackConsumer`) consumes the feedback:
   - on `MailSentEvent` → `markSagaCompleted`,
   - on `MailFailedEvent` → `failSaga` — the after-commit hook fires `SagaCompensationRunner.runCompensation`, which calls `IamSagaCompensator` to publish compensation actions to `saga-compensation-iam` (also via outbox); `SagaCompensationService` listens and delegates to `AuthCompensationService` (rollback Keycloak user, delete verification token, revert email/password change).

Compensation only exists where effects are reversible — `mail-service` therefore does **not** have a `SagaCompensationService` (a sent email cannot be un-sent).

### Event-Driven Communication

Services communicate asynchronously through Kafka events. Integration events are defined as **Avro** schemas under `contracts/<service>/<topic>/v<n>/*.avsc` and serialized in binary form with Schema Registry:
- **`MailRequestedEvent`** — published by `iam-service` (via `AuthEventPublisherPort` / `KafkaAuthEventPublisherAdapter`), consumed by `mail-service` (`MailEventConsumer`).
- **`MailSentEvent`** / **`MailFailedEvent`** — published by `mail-service` through the Transactional Outbox, consumed by `iam-service` (`MailFeedbackConsumer`) to advance or fail the originating saga.
- **Saga compensation actions** — published by `iam-service` to its own internal `saga-compensation-iam` topic as an Avro **tagged union** (`DeleteUserAction`, `DeleteVerificationTokenAction`, `RevertPasswordUpdateAction`, `RevertEmailUpdateAction`), consumed by `SagaCompensationService` and dispatched to `AuthCompensationService` after being decoded by `AvroAuthCompensationCommandTranslator` (ACL) into a typed `sealed interface AuthCompensationCommand`. No `Map<String, Any?>` envelope, no stringly-typed `action` discriminator — exhaustive `when` at compile time.

All publishing goes through the Outbox (`KafkaOutboxProcessor.saveOutboxMessage(topic, key, payload: ByteArray, ...)`); all consumption goes through `ProcessedEventGuard` for idempotency. Event publishing and consuming are handled by dedicated adapters in `infrastructure/kafka/`, keeping the domain core focused on business logic.

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
- Entities use `@Version` to enable Optimistic Locking (e.g., `User`, `Role`, `OutboxJpaEntity`, `BaseSaga`, `BaseSagaStep`).
- Services follow the pattern: load the entity → invoke a behavior method (e.g. `saga.markCompleted()`) → `save(...)` inside `@Transactional`. Hibernate includes `WHERE version = ?` and raises a conflict if data changed concurrently.

### Sagas (Internal, no ETag)
- Sagas are backend-internal processes (event-driven), not HTTP resources — therefore **ETag/If-Match is not used in Sagas**.
- Concurrency control:
    - `@Version` on `BaseSaga` and `BaseSagaStep` (load → behavior method → save), so Hibernate emits `WHERE version = ?` and raises a conflict if rows changed concurrently.
    - Idempotency for steps:
        - Database-level unique constraint on `(sagaId, stepName)` prevents duplicate step insertion.
        - Service-level soft check in `SagaEngine.recordSagaStep` returns the existing step if already present (safe retries/duplicates).
        - Consumer-side `ProcessedEventGuard` short-circuits any event already processed by the same consumer group.
- Compensation runs in a separate `REQUIRES_NEW` transaction inside `SagaCompensationRunner` and is scheduled with an *after-commit* hook, so a rolled-back business transaction never triggers a stray compensation. `SagaWatchdog` retries stuck `COMPENSATING` / `COMPENSATION_FAILED` sagas with a cooldown (`veds.saga.compensation-retry-cooldown`).

### Outbox Pattern
- `OutboxJpaEntity` has `@Version` and a UNIQUE constraint on `event_id`.
- The poller (`KafkaOutboxProcessor`) and the transactional helper (`OutboxDispatchTx`) implement a two-phase claim/dispatch:
    1. Claim batch: short tx, `SELECT … FOR UPDATE SKIP LOCKED` (so multiple instances never grab the same row) → flip `READY → PROCESSING`.
    2. Dispatch: Kafka send happens *outside* of any DB transaction; result is recorded via `markCompleted` or `markRetryScheduled`/`markDeadLettered` in a fresh `REQUIRES_NEW` transaction (Optimistic Locking still guards each row).
- Retries: `retryCount` is bounded by `veds.outbox.max-retries`; exceeding it transitions the row to `DEAD_LETTERED` (manual intervention).
- Stuck-message reaper: a row stuck in `PROCESSING` longer than `veds.outbox.stuck-threshold` (publisher crash) becomes claimable again.
- All knobs externalized in `KafkaOutboxProperties` (`veds.outbox.*`); saga timing in `SagaProperties` (`veds.saga.*`).

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

You can also use the **Quality: ktlintFormat (all)** / **Quality: ktlintCheck + detekt (all)** run configurations from IntelliJ.
