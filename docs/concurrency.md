# Optimistic Locking & Concurrency Control

This project uses a combination of HTTP ETags (at API boundaries) and JPA Optimistic Locking (within services) to prevent lost updates and to ensure safe concurrency across microservices and asynchronous processing.

## Summary

| Layer                 | Mechanism                                                                                               |
|-----------------------|---------------------------------------------------------------------------------------------------------|
| **API layer**         | ETag/If-Match for conditional updates from clients (front-end, API consumers).                          |
| **Persistence layer** | JPA `@Version` on entities and the load → mutate → save pattern inside a single transaction.            |
| **Sagas and Outbox**  | Internal consistency ensured by JPA Optimistic Locking and idempotency safeguards — no HTTP ETags here. |

### Error Semantics at the API Layer

The API layer returns specific HTTP status codes to handle concurrency and versioning errors.

| Status Code                     | Condition                                                                                             |
|---------------------------------|-------------------------------------------------------------------------------------------------------|
| **`428 Precondition Required`** | Missing `If-Match` on required endpoints.                                                             |
| **`412 Precondition Failed`**   | ETag/If-Match does not match the current version.                                                     |
| **`409 Conflict`**              | Last-resort handler for JPA `ObjectOptimisticLockingFailureException` (race detected at commit time). |

## Persistence: JPA Optimistic Locking

The persistence layer relies on JPA mechanisms to ensure data consistency during concurrent modifications.

| Aspect                | Implementation                                                                                                                                                                                                                                     |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Entity Versioning** | Entities use `@Version` annotation to enable Optimistic Locking.                                                                                                                                                                                   |
| **Service Pattern**   | Services follow the pattern: load the entity → invoke a behavior method (e.g. `saga.markCompleted()`) → `save(...)` inside `@Transactional` annotation. Hibernate includes `WHERE version = ?` and raises a conflict if data changed concurrently. |

## Sagas (Internal, no ETag)

Sagas are backend-internal processes (event-driven), not HTTP resources — therefore **ETag/If-Match is not used in Sagas**. Concurrency control is managed through the following mechanisms:

| Mechanism                 | Implementation Details                                                                                                                                                                                                                                                                                                                                    |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Entity Versioning**     | `@Version` on `BaseSaga` and `BaseSagaStep` (load → behavior method → save), so Hibernate emits `WHERE version = ?` and raises a conflict if rows changed concurrently.                                                                                                                                                                                   |
| **Database Idempotency**  | Database-level unique constraint on `(sagaId, stepName)` prevents duplicate step insertion.                                                                                                                                                                                                                                                               |
| **Service Idempotency**   | Service-level soft check in `SagaEngine.recordSagaStep` returns the existing step if already present (safe retries/duplicates).                                                                                                                                                                                                                           |
| **Consumer Idempotency**  | Consumer-side `ProcessedEventGuard` short-circuits any event already processed by the same consumer group.                                                                                                                                                                                                                                                |
| **Compensation Handling** | Compensation runs in a separate `REQUIRES_NEW` transaction inside `SagaCompensationRunner` and is scheduled with an *after-commit* hook, so a rolled-back business transaction never triggers a stray compensation. `SagaWatchdog` retries stuck `COMPENSATING` or `COMPENSATION_FAILED` sagas with a cooldown (`veds.saga.compensation-retry-cooldown`). |

## Outbox Pattern

The Transactional Outbox pattern guarantees reliable event publication with robust concurrency controls and retry mechanisms.

| Feature                  | Details                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Entity Versioning**    | `OutboxJpaEntity` has `@Version` and a UNIQUE constraint on `event_id`.                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Two-Phase Dispatch**   | The poller (`KafkaOutboxProcessor`) and the transactional helper (`OutboxDispatchTx`) implement a two-phase claim/dispatch flow:<br>1. **Claim batch**: short tx, `SELECT … FOR UPDATE SKIP LOCKED` (so multiple instances never grab the same row) → flip `READY → PROCESSING`.<br>2. **Dispatch**: Kafka send happens *outside* any DB transaction; result is recorded via `markCompleted` or `markRetryScheduled`/`markDeadLettered` in a fresh `REQUIRES_NEW` transaction (Optimistic Locking still guards each row). |
| **Retries**              | `retryCount` is bounded by `veds.outbox.max-retries`; exceeding it transitions the row to `DEAD_LETTERED` (manual intervention).                                                                                                                                                                                                                                                                                                                                                                                          |
| **Stuck-Message Reaper** | A row stuck in `PROCESSING` longer than `veds.outbox.stuck-threshold` (publisher crash) becomes claimable again.                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Configuration**        | All knobs externalized in `KafkaOutboxProperties` (`veds.outbox.*`); saga timing in `SagaProperties` (`veds.saga.*`).                                                                                                                                                                                                                                                                                                                                                                                                     |
