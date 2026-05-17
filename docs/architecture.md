# Architecture

## Overview

A microservices-based architecture following Domain-Driven Design principles, Hexagonal Architecture, Separation of Concerns, SOLID principles, Choreography pattern for service coordination, and the Saga pattern for distributed transactions.

The project is split into the following components:

1. **API Gateway** – Entry point for all client requests, handles routing to appropriate services, JWT validation, and BFF (Backend-For-Frontend) auth proxy to Keycloak.
2. **IAM Service** – Consolidates user management, roles, permissions, and account operations. Authentication is delegated to Keycloak.
3. **Mail Service** – Handles email sending operations and templates.
4. **Shared Infrastructure** – Engines, contracts, and utilities used across all microservices.
5. **`iam-contracts`, `mail-contracts`, `template-contracts`** – Per-bounded-context Avro Published Language modules. Each holds only Avro `*.avsc` schemas and the Java `SpecificRecord` classes generated from them.
6. **Template Service** – Baseline configuration for future microservices.

Each microservice follows Hexagonal Architecture principles with a three-layer structure and has its own PostgreSQL database. Services communicate with each other asynchronously via Apache Kafka (event-driven, choreography-based), with the Transactional Outbox pattern guaranteeing reliable event publication.

## Components

### Kafka KRaft Mode

- The system uses Kafka in KRaft mode (Kafka Raft), eliminating the need for Zookeeper.
- Configuration is handled via `KAFKA_PROCESS_ROLES` (broker, controller) and `KAFKA_CONTROLLER_QUORUM_VOTERS`.
- A static `CLUSTER_ID` is provided in `docker-compose.yml` for simplified setup.

### Shared Infrastructure

- Provides shared engines, event definitions, and utilities for all microservices.
- Implements **persistence-agnostic contracts** for the Transactional Outbox and Saga patterns (`OutboxRepositoryPort`, `ProcessedEventRepositoryPort`, `SagaRepositoryPort`, `SagaStepRepositoryPort`) — any database (PostgreSQL, MongoDB, …) can plug in by implementing the ports.
- Reusable Kafka building blocks: `KafkaOutboxProcessor` + `OutboxDispatchTx` (two-phase claim/dispatch), `ProcessedEventGuard` (idempotent receiver), `AvroPayloadSerializer`/`AvroPayloadDeserializer`.
- Shared **technical IdP adapter** for OAuth2/OIDC: `KeycloakJwtAuthenticationConverter` (servlet) and `ReactiveKeycloakJwtAuthenticationConverter` (WebFlux) — wired via `KeycloakSecurityAutoConfiguration`. They translate the configured roles claim (`veds.shared.keycloak.roles-claim-path`) into Spring Security `ROLE_*` authorities without knowing any concrete role names — so they are role-vocabulary-agnostic and remain in `shared-infrastructure` as a pure technical adapter (see *Where do role names live?* in [Keycloak configuration](./keycloak.md)).
- Saga engine: generic `SagaEngine`, `SagaCompensationRunner`, `SagaWatchdog` operating on F-bounded contracts (`Saga<S : Saga<S>>`, `SagaStep<T : SagaStep<T>>`) — zero unchecked casts in the engine.
- Externalized configuration: `KafkaOutboxProperties` (`veds.outbox.*`) and `SagaProperties` (`veds.saga.*`) auto-registered by `OutboxAndSagaAutoConfiguration`.
- Composable contracts for inter-service conventions: `SagaCompensationTopic.PREFIX` (each service composes its own `saga-compensation-<participant>` topic).
- Integration event schemas defined as Avro (`contracts/<service>/<topic>/v<n>/*.avsc`) — binary wire format with Schema Registry.

### Avro Published Language Modules

- **Why a separate module per bounded context:**
  Avro schemas owned by a service end up as generated `SpecificRecord` Java classes. Generating them inside the consuming service produced a classic Spring Boot DevTools pitfall: the generated classes were loaded by `RestartClassLoader` while collaborator code in `shared-infrastructure` used the application classloader, yielding `ClassCastException: X cannot be cast to X` at runtime.
  Lifting the contracts into their own composite-build modules ships them as plain library JARs, so they always resolve via the application classloader — the issue cannot recur even when DevTools is added.
- **What each module contains:** a single `java-library` Gradle build that runs `avro-tools` over the `*.avsc` files and produces a `SpecificRecord` JAR (`api("org.apache.avro:avro:...")` so consumers get the runtime transitively).
- **Where the schemas live:**
    - `iam-contracts` / `mail-contracts` read schemas from the repository-root `contracts/<service>/**` directory — single source of truth shared with the Terraform topic provisioner and the Schema Registry registration script.
    - `template-contracts` keeps its schemas **locally** (`template-contracts/avro/**`) — the template service is intentionally excluded from production schema/topic provisioning. When cloning, move the schemas under `contracts/<new-service>/`.
- **Anti-Corruption Layer (ACL):** generated Avro types **never** leave the `infrastructure/saga/` package. A dedicated translator (`AvroAuthCompensationCommandTranslator`, `AvroTemplateCompensationCommandTranslator`) decodes raw bytes into a Kotlin `sealed interface` (`AuthCompensationCommand`, `TemplateCompensationCommand`) living in `application/saga/model/`. The application layer therefore stays Avro, Jackson, Kafka and Spring free, and compensation dispatch is an exhaustive `when` over a typed hierarchy (no `Map<String, Any?>`, no stringly-typed `action` discriminators, compile-time-checked).
- **DevTools:** intentionally not declared in `iam-service/infrastructure`, `mail-service/infrastructure`, `template-service/infrastructure`.

### IAM Service

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
- Consumes feedback events from mail-service (`MailSentEvent` / `MailFailedEvent`) and runs compensation logic via a dedicated `saga-compensation-iam` Kafka topic + `AuthCompensationService` use case (rollback user in Keycloak, delete verification token, revert email/password change). The compensation envelope is an Avro **tagged union** (`DeleteUserAction | DeleteVerificationTokenAction | RevertPasswordUpdateAction | RevertEmailUpdateAction`) decoded by an ACL translator into the application-layer `sealed interface AuthCompensationCommand`.

### Mail Service

- Responsible for sending emails based on Thymeleaf templates.
- Database stores:
    - Email logs and delivery status (`email_log` table).
    - Local saga state (`saga`, `saga_step` tables).
- Consumes `MailRequestedEvent` events from other services (`MailEventConsumer`).
- Publishes `MailSentEvent` / `MailFailedEvent` back through the Transactional Outbox (`KafkaOutboxProcessor`).
- Supports various email templates (welcome, password reset, account activation, email change confirmation, etc.).
- Acts as a reactive participant in the choreography flow — has no outbound business-event port (publication is a saga mechanic, not a domain concern).
- Uses hexagonal architecture to decouple email sending logic from the SMTP/mail provider.

### Template Service

- A baseline skeleton for spinning up a new microservice.
- Excluded from the global `contracts/` directory. Its Avro schemas live locally in a paired Published Language module `template-contracts/avro/` so they are **not** picked up by the production provisioner image or Terraform topic provisioning.
- Provides:
    - Hexagonal package layout (`domain/model`, `domain/repository`, `application/service`, `application/saga/{model,port}`, `application/port/{inbound,out}`, `infrastructure/{persistence,kafka,saga,web,config,response,exception}`).
    - Placeholder domain (`Template`, `TemplateStatus`, `TemplateRepository`) with rich-domain methods (`markProcessed`, `markFailed`).
    - Transactional Outbox scaffolding: local JPA entity implementing the shared read-only `OutboxMessage` contract, ensuring guaranteed decoupled event publication.
    - Saga choreography scaffolding: local `saga`/`saga_step` model, `SagaProcessPort`, thin `SagaManagerAdapter` delegating to a shared `SagaEngine` (composition over inheritance), neutral compensation topic `saga-compensation-template`.
    - **Typed compensation pipeline** mirroring `iam-service`: sealed `TemplateCompensationCommand` in the application layer, `AvroTemplateCompensationCommandTranslator` (ACL) in infrastructure, `TemplateCompensationEventSerializer`, `TemplateSagaCompensator`, `TemplateSagaCompensationHandler`, `SagaCompensationService` (Kafka inbound) and a placeholder `TemplateCompensationService` use case to wire from on clone.
    - Kafka skeleton: outbound `TemplateEventPublisherPort` + `KafkaTemplateEventPublisherAdapter` (using `KafkaOutboxProcessor`), inbound `TemplateEventConsumer`.
    - REST controller `/template` + DTOs.
- **When cloning**: copy `template-service/` **and** `template-contracts/`, rename packages and module names, **move `template-contracts/avro/*.avsc` into the shared `contracts/<new-service>/`** so the new service participates in global schema registration and topic provisioning, point its `template-contracts/build.gradle.kts` at the new location, and add `includeBuild("<new-service>")` + `includeBuild("<new-service>-contracts")` lines to the root `settings.gradle.kts`.

## Design Principles

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

### SOLID Principles and Separation of Concerns

The codebase adheres to:
- **Single Responsibility Principle**: Each class has a single responsibility.
- **Open/Closed Principle**: Classes are open for extension but closed for modification.
- **Liskov Substitution Principle**: Subtypes are substitutable for their base types.
- **Interface Segregation Principle**: Specific interfaces rather than general ones.
- **Dependency Inversion Principle**: Depends on abstractions (ports), not concretions (adapters).

The hexagonal architecture naturally enforces these principles by isolating business logic from external concerns, using dependency inversion through ports and adapters, and maintaining clear boundaries between layers.

### Choreography Pattern for Service Coordination

The system uses a choreography-based approach for service coordination:
- Services react to events published by other services without central coordination.
- Each service knows which events to listen for and what actions to take.
- No central orchestrator is needed, making the system more decentralized and resilient.
- Services maintain autonomy and can evolve independently.

Benefits: reduced coupling, flexible and scalable architecture, easier to add/modify services, better resilience (no single point of failure), and natural alignment with hexagonal architecture.