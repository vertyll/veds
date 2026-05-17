# Saga Pattern & Transactional Outbox

## Overview

The system uses **choreography-based sagas** — there is no central orchestrator. Each participating service runs its own local saga and progresses by reacting to domain events on Kafka.

## Polyglot Persistence via Shared Contracts

Both the **Saga** and **Transactional Outbox** patterns are built on top of database-agnostic ports defined in `shared-infrastructure`:

| Contract                                         | Purpose                                                                                                                        |
|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `Saga<S : Saga<S>>`, `SagaStep<T : SagaStep<T>>` | Rich-aggregate ports with F-bounded generics — behavior methods return the concrete adapter type, eliminating unchecked casts. |
| `SagaRepositoryPort`, `SagaStepRepositoryPort`   | Persistence ports for sagas.                                                                                                   |
| `OutboxMessage`, `OutboxRepositoryPort`          | Outbox aggregate + repository port (with `lockBatchForDispatch` for `SELECT … FOR UPDATE SKIP LOCKED`).                        |
| `ProcessedEventRepositoryPort`                   | Idempotent-receiver ledger (UNIQUE `(eventId, consumerGroup)`).                                                                |

`shared-infrastructure` ships JPA flavors (`BaseSaga`, `BaseSagaStep`, `BaseSagaRepository`, `BaseSagaStepRepository`, `OutboxJpaEntity`) — to introduce a different storage (MongoDB, DynamoDB, …) you only implement the ports against the new technology; the engines do not change.

## Canonical Building Blocks

All building blocks live in `shared-infrastructure`.

### 1. Transactional Outbox

`KafkaOutboxProcessor` (poller) + `OutboxDispatchTx` (transactional helper):

- **Two-phase dispatch**: a short transaction *claims* a batch with `SELECT … FOR UPDATE SKIP LOCKED` and flips rows to `PROCESSING`; Kafka publication happens *outside* any DB transaction; success/failure is recorded in a fresh `REQUIRES_NEW` transaction (`markCompleted` / `markRetryScheduled` / `markDeadLettered`).
- **Statuses**: `READY → PROCESSING → COMPLETED` (happy path) or `→ DEAD_LETTERED` after exhausting retries (`OutboxStatus`).
- **UNIQUE constraint** on `event_id` enforces producer-side idempotency.
- **Stuck-message reaper** rescues `PROCESSING` rows abandoned by a crashed publisher (configurable threshold).
- **All knobs externalized** via `KafkaOutboxProperties` (`veds.outbox.poll-interval | batch-size | max-retries | retry-cooldown | stuck-threshold`).

### 2. Idempotent Receiver

`ProcessedEventGuard.claim(eventId, consumerGroup)` writes to a ledger with UNIQUE `(eventId, consumerGroup)`; duplicate deliveries are detected via caught `DataIntegrityViolationException` and short-circuited (Kafka's at-least-once delivery is neutralized at the consumer boundary).

### 3. Saga Engine

Generic `SagaEngine<S, T>` with choreography semantics:

- `startSaga` / `recordSagaStep` / `awaitResponse` / `completeSaga` / `failSaga`.
- On step failure or explicit `failSaga`, an *after-commit* hook (`TransactionSynchronizationManager`) delegates to `SagaCompensationRunner.runCompensation` which opens its own `REQUIRES_NEW` transaction — so compensation only runs if the caller's business transaction actually commits, and runs through Spring's AOP proxy (no self-invocation pitfalls).

### 4. Saga Watchdog

`SagaWatchdog` (`@Scheduled`, `veds.saga.watchdog-interval`):

- Times out sagas stuck in `AWAITING_RESPONSE` longer than `veds.saga.await-response-timeout` (calls `failSaga`).
- Retries compensation for sagas in `COMPENSATING` / `COMPENSATION_FAILED` whose `updatedAt` is older than `veds.saga.compensation-retry-cooldown`.

### 5. Cross-Service Compensation Topic Naming

`SagaCompensationTopic.PREFIX + "<service>"` — each service composes its own neutral topic (e.g. `saga-compensation-iam`).

### 6. Saga Log Correlation

Feedback events carry `sagaId`; the originating saga sits in `AWAITING_RESPONSE` until matched.

### 7. Recovery via Scheduled Jobs

Service-local `SchedulingConfig` wires `@EnableScheduling` so `KafkaOutboxProcessor` and `SagaWatchdog` ticks fire.

## Example: User Registration (iam-service ⇄ mail-service)

1. `iam-service` (`AuthService.register`) begins a local saga `USER_REGISTRATION`, records steps `CREATE_USER` → `PUBLISH_USER_REGISTERED_EVENT` → `CREATE_VERIFICATION_TOKEN` → `PUBLISH_MAIL_REQUESTED_EVENT`, transitions to `AWAITING_RESPONSE`, and inserts a `MailRequestedEvent` row into the outbox (same JDBC transaction as the user write — no dual-write).
2. The outbox poller publishes the Avro-serialized `MailRequestedEvent` to Kafka.
3. `mail-service` (`MailEventConsumer`) consumes the event, claims it via `ProcessedEventGuard`, begins its own local saga `EMAIL_SENDING`, performs `SEND_EMAIL`, completes its saga, and inserts `MailSentEvent` (or `MailFailedEvent`) into its outbox.
4. `iam-service` (`MailFeedbackConsumer`) consumes the feedback:
    - on `MailSentEvent` → `markSagaCompleted`.
    - on `MailFailedEvent` → `failSaga` — the after-commit hook fires `SagaCompensationRunner.runCompensation`, which calls `IamSagaCompensator` to publish compensation actions to `saga-compensation-iam` (also via outbox); `SagaCompensationService` listens and delegates to `AuthCompensationService` (rollback Keycloak user, delete verification token, revert email/password change).

> **Note:** Compensation only exists where effects are reversible — `mail-service` therefore does **not** have a `SagaCompensationService` (a sent email cannot be un-sent).

## Event-Driven Communication

Services communicate asynchronously through Kafka events. Integration events are defined as **Avro** schemas under `contracts/<service>/<topic>/v<n>/*.avsc` and serialized in binary form with Schema Registry:

- **`MailRequestedEvent`** — published by `iam-service` (via `AuthEventPublisherPort` / `KafkaAuthEventPublisherAdapter`), consumed by `mail-service` (`MailEventConsumer`).
- **`MailSentEvent`** / **`MailFailedEvent`** — published by `mail-service` through the Transactional Outbox, consumed by `iam-service` (`MailFeedbackConsumer`) to advance or fail the originating saga.
- **Saga compensation actions** — published by `iam-service` to its own internal `saga-compensation-iam` topic as an Avro **tagged union** (`DeleteUserAction`, `DeleteVerificationTokenAction`, `RevertPasswordUpdateAction`, `RevertEmailUpdateAction`), consumed by `SagaCompensationService` and dispatched to `AuthCompensationService` after being decoded by `AvroAuthCompensationCommandTranslator` (ACL) into a typed `sealed interface AuthCompensationCommand`. No `Map<String, Any?>` envelope, no stringly-typed `action` discriminator — exhaustive `when` at compile time.

All publishing goes through the Outbox (`KafkaOutboxProcessor.saveOutboxMessage(topic, key, payload: ByteArray, ...)`); all consumption goes through `ProcessedEventGuard` for idempotency.
