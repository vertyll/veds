<p align="center">
<img alt="" src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
<img alt="" src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
<img alt="" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
</p>

## Project Assumptions

A microservices-based architecture for Project A, following Domain-Driven Design (DDD) principles, Hexagonal Architecture (Ports & Adapters), Separation of Concerns (SoC), SOLID principles, Choreography pattern for service coordination, and the Saga pattern for distributed transactions.

## Architecture

![Project architecture graph](https://raw.githubusercontent.com/vertyll/project-a-microservices/refs/heads/main/screenshots/project-architecture-graph.png)

The project is split into the following components:

1. **API Gateway** - Entry point for all client requests, handles routing to appropriate services and JWT token validation
2. **Identity Service** - Consolidates authentication, user management, and roles/permissions into a single service (port 8082)
3. **Mail Service** - Handles email sending operations and templates (port 8085)
4. **Shared Infrastructure** - Shared infrastructure, contracts, and utilities used across all microservices
5. **Template Service** - Baseline configuration for future microservices

Each microservice follows Hexagonal Architecture principles with a three-layer structure and has its own PostgreSQL database. Services communicate with each other via Apache Kafka for event-driven architecture, primarily for notifying the Mail Service.

### Detailed Description of Components

#### Kafka KRaft Mode
- The system uses Kafka in KRaft mode (Kafka Raft), eliminating the need for Zookeeper.
- Configuration is handled via `KAFKA_PROCESS_ROLES` (broker,controller) and `KAFKA_CONTROLLER_QUORUM_VOTERS`.
- A static `CLUSTER_ID` is provided in `docker-compose.yml` for simplified setup.

#### Shared Infrastructure
- Provides shared code, DTOs, event definitions, and utilities for all microservices
- Implements shared patterns like Outbox pattern
- Contains reusable components for Kafka integration, exception handling, and API responses
- Ensures consistency in how services communicate and process events
- Includes base classes for implementing event choreography
- Provides shared ports and adapters interfaces for consistent hexagonal architecture implementation
- Integration events definitions for inter-service communication

#### Identity Service
- Responsible for user authentication, authorization, profile management, and roles.
- Uses JWT and refresh tokens (http-only secure cookies) for session management.
- Database stores:
    - User credentials (email, hashed passwords)
    - User profile data (first name, last name, preferences)
    - Role definitions and user-role assignments
    - Refresh tokens and verification tokens
- Provides endpoints for registration, login, profile management, and role administration.
- Publishes mail request events to trigger email workflows.
- Implements internal Saga pattern for multi-step operations like user registration.

#### Mail Service
- Responsible for sending emails based on templates
- Database stores:
    - Email logs
    - Delivery status
- Consumes mail request events from other services
- Supports various email templates (welcome, password reset, account activation, etc.)
- Acts as a reactive service in the choreography flow
- Uses hexagonal architecture to decouple email sending logic from external mail providers

#### Template Service
- A template for implementing hexagonal architecture
- Example domain models, repositories, and services
- Sample event definitions and Kafka integration
- Basic Saga pattern implementation

## Technology Stack

- **Back-end**: Spring Boot, Kotlin, Gradle Kotlin DSL
- **Database**: PostgreSQL (separate instance for each service)
- **Message Broker**: Apache Kafka KRaft (Zookeeper-less)
- **API Documentation**: OpenAPI (Swagger)
- **Containerization**: Docker/Podman, Docker Compose/Podman Compose
- **Authentication**: JWT and refresh tokens (http only secure cookie)
- **Testing**: JUnit, Testcontainers

## Development Setup

### Prerequisites

- Docker and Docker Compose / Podman and Podman Compose
- JDK
- Gradle

### Getting Started

1. Clone the repository:
```bash
git clone https://github.com/yourusername/project-a-microservices.git
# and
cd project-a-microservices
```

2. Start the infrastructure:
```bash
docker-compose up -d
```

3. Build and run microservices:
   Each microservice is now an independent Gradle project. To build or run a specific service, navigate to its directory:

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
- Identity Service: http://localhost:8082
- Mail Service: http://localhost:8085
- Kafka UI: http://localhost:8090
- MailDev: http://localhost:1080

### Global Management (Convenience)

For convenience, you can use the provided `Makefile` in the root directory:

- Build all services: `make build-all`
- Run tests in all services: `make test-all`
- Clean all services: `make clean-all`

### Development Workflow

1. Make changes to the relevant service code
2. Build the service:
```bash
cd <service-name>
./gradlew build
```
3. Start the microservice:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Architecture Design

### Hexagonal Architecture (Ports & Adapters)

Each microservice implements Hexagonal Architecture to achieve clean separation of concerns.

Benefits:
- **Testability**: Core business logic can be tested independently
- **Flexibility**: Easy to swap external dependencies without affecting business logic
- **Maintainability**: Clear separation between business rules and technical details
- **Technology Independence**: Core domain is not coupled to specific frameworks or technologies

### Domain-Driven Design (DDD)

Each microservice is designed around a specific business domain with:
- A clear bounded context
- Domain models that represent business entities
- A layered architecture within the hexagonal structure
- Domain-specific language
- Encapsulated business logic

The project structure follows DDD principles within hexagonal architecture:
- `domain`: Core business models, domain services, domain events, and business logic
- `application`: Controllers, DTOs, use cases, application services, and port definitions (Primary Adapters)
- `infrastructure`: Adapters for external systems like databases, Kafka, email providers, etc. (Secondary Adapters)

### SOLID Principles and Separation of Concerns

The codebase adheres to:
- **Single Responsibility Principle**: Each class has a single responsibility
- **Open/Closed Principle**: Classes are open for extension but closed for modification
- **Liskov Substitution Principle**: Subtypes are substitutable for their base types
- **Interface Segregation Principle**: Specific interfaces rather than general ones
- **Dependency Inversion Principle**: Depends on abstractions (ports), not concretions (adapters)

The hexagonal architecture naturally enforces these principles by:
- Isolating business logic from external concerns
- Using dependency inversion through ports and adapters
- Maintaining clear boundaries between layers

### Choreography Pattern for Service Coordination

The system uses a choreography-based approach for service coordination:

- Services react to events published by other services without central coordination
- Each service knows which events to listen for and what actions to take
- No central orchestrator is needed, making the system more decentralized and resilient
- Services maintain autonomy and can evolve independently

Benefits of this approach:
- Reduced coupling between services
- More flexible and scalable architecture
- Easier to add new services or modify existing ones
- Better resilience as there's no single point of failure
- Aligns well with hexagonal architecture by treating event communication as external adapters

### Saga Pattern for Distributed Transactions

For distributed transactions, we use the Saga pattern (currently internal to Identity Service but can be extended):

1. A service or component initiates a multi-step operation (e.g., registration).
2. Each step is recorded in the Saga state machine.
3. If a step fails, compensating transactions are triggered to maintain consistency.

Example: User Registration Saga (Internal to Identity Service)
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
- Persistence layer: JPA `@Version` on entities + the load → mutate → save pattern inside a single transaction.
- Sagas and Outbox: Internal consistency ensured by JPA Optimistic Locking and idempotency safeguards — no HTTP ETags here.

### API: ETag / If-Match
- Identity Service
    - `PUT /users/{id}` (Update profile) - returns and optionally accepts `If-Match`.
    - `POST /roles/user/{userId}/role/{roleName}` (Assign role) and `DELETE /roles/user/{userId}/role/{roleName}` (Remove role) optionally accept `If-Match` for the **User** entity.
    - `GET /roles/{id}`, `GET /roles/name/{name}` return `ETag: W/"<version>"`.
    - `GET /users/{id}`, `GET /users/email/{email}` return `ETag: W/"<version>"` for clients that want to track staleness.

Example client flow (Profile update):
1. Read current state
```
curl -i http://localhost:8082/users/1
# Response contains: ETag: W/"<version>"
```
2. Update with precondition
```
curl -i -X PUT http://localhost:8082/users/1 \
  -H 'Content-Type: application/json' \
  -H 'If-Match: W/"<version>"' \
  -d '{"firstName": "John", "lastName": "Doe", "profilePicture": "http://...", "phoneNumber": "123456789", "address": "123 St"}'
```
- If the resource changed meanwhile, the server returns `412`.

Error semantics at the API layer:
- `428 Precondition Required` — missing `If-Match` on required endpoints.
- `412 Precondition Failed` — ETag/If-Match does not match current version.
- `409 Conflict` — last-resort handler for JPA `ObjectOptimisticLockingFailureException` (race detected at commit time).

### Persistence: JPA Optimistic Locking
- Entities use `@Version` to enable Optimistic Locking (e.g., `User`, `Role`, `KafkaOutbox`, and Saga/SagaStep entities where applicable).
- Services follow the pattern: load the entity → apply changes → `save(...)` inside `@Transactional`. Hibernate includes `WHERE version = ?` and raises a conflict if data changed concurrently.

### Sagas (Internal, no ETag)
- Sagas are backend-internal processes (event-driven), not HTTP resources — therefore **ETag/If-Match is not used in Sagas**.
- Concurrency control:
    - `@Version` on Saga and/or SagaStep where applicable, persisted via load → mutate → save.
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

- Identity Service: http://localhost:8082/swagger-ui.html
- Mail Service: http://localhost:8085/swagger-ui.html

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
