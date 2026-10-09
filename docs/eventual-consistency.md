# Eventual Consistency: Outbox, Inbox and Sagas

## Overview

No transaction spans two services here — there is no 2PC and no XA. Local ACID transactions do the work, and three
mechanisms carry state between them:

| Mechanism  | Guarantees                                                                                          |
|------------|-----------------------------------------------------------------------------------------------------|
| **Outbox** | a state change and the event announcing it commit together, so neither can happen without the other |
| **Inbox**  | a delivery is handled once, and a failed handler leaves nothing claimed                             |
| **Saga**   | a workflow spanning services completes or is compensated                                            |

A saga is not a transaction: it has no atomicity and no isolation. Intermediate states are visible, and undo is a new
business action rather than a `ROLLBACK`. What it buys is that a workflow never stops halfway with nobody responsible
for it.

The sagas are **choreography-based** — there is no central orchestrator. Each participating service runs its own local
saga and progresses by reacting to domain events on Kafka.

---

## Polyglot Persistence via Shared Contracts

Both the **Saga** and **Transactional Outbox** patterns are built on top of database-agnostic ports — the saga ports in
`shared-saga-engine`, the outbox ports in `shared-messaging-kafka`. To introduce a different storage (MongoDB,
PostgreSQL, …), you only implement the ports against the new technology; the engines do not change.

The JPA flavor is shared, not copied. `shared-messaging-kafka` owns one `OutboxEntity`, one
`ProcessedEventEntity`, their repositories and the adapters that bind them to the ports. Every
service maps that one pair onto its own `kafka_outbox` and `processed_event` tables, created by
its own migration — the table is per-database, the mapping is not. A service states which halves
it carries by naming the packages in `@EntityScan` and `@EnableJpaRepositories`; one that
publishes nothing scans the inbox alone and excludes the outbox beans, as `translation-service`
does.

| Contract                                            | Purpose                                                                                                                        |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `Saga<S : Saga<S>>`<br/>`SagaStep<T : SagaStep<T>>` | Rich-aggregate ports with F-bounded generics — behavior methods return the concrete adapter type, eliminating unchecked casts. |
| `SagaRepositoryPort`<br/>`SagaStepRepositoryPort`   | Persistence ports for sagas.                                                                                                   |
| `OutboxMessage`<br/>`OutboxRepositoryPort`          | Outbox aggregate + repository port (with `lockBatchForDispatch` for `SELECT … FOR UPDATE SKIP LOCKED`).                        |
| `ProcessedEventRepositoryPort`                      | Idempotent-receiver ledger (UNIQUE `(eventId, consumerGroup)`).                                                                |

---

## Canonical Building Blocks

All building blocks live in `shared-messaging-kafka` and `shared-saga-engine`, and provide the foundation for robust
asynchronous processing.

### Transactional Outbox

Consists of `KafkaOutboxProcessor` (poller) and `OutboxDispatchTx` (transactional helper).

- **Two-phase dispatch** — Claims a batch with `SELECT … FOR UPDATE SKIP LOCKED` (`READY → PROCESSING`). Kafka
  publication happens *outside* any DB transaction. Success/failure is recorded in a fresh `REQUIRES_NEW` transaction
  (`COMPLETED`, `markRetryScheduled`, `markDeadLettered`).
- **Idempotency** — UNIQUE constraint on `event_id`.
- **Reaper** — Rescues stuck `PROCESSING` rows abandoned by a crash.
- **Config** — Externalized via `KafkaOutboxProperties`.

### Inbox — Idempotent Receiver

`ProcessedEventGuard.claim(eventId, consumerGroup)` writes to a ledger with a UNIQUE constraint on
`(eventId, consumerGroup)`. A delivery whose id is already there is skipped.

The claim is written **in the handler's own transaction**, so the two share a fate. That is what makes the
retry and dead-letter machinery mean anything:

| Handler outcome | Claim                              | Redelivery                                                  |
|-----------------|------------------------------------|-------------------------------------------------------------|
| commits         | committed with the business writes | skipped as a duplicate                                      |
| throws          | rolled back with them              | a real retry, and after `MAX_RETRIES` the dead letter topic |

A claim committed independently would invert the second row: every retry would find its own claim, report a
duplicate and return successfully, so a transient failure would drop the message on its first attempt and
nothing would ever reach the dead letter topic.

Delivery is therefore **at-least-once**, and every `@KafkaListener` that claims is `@Transactional` so the
claim and the handler share one unit of work. A handler must tolerate being re-entered after a failure.

Two consumers racing on the same event both pass the existence check; one loses on the UNIQUE constraint and
rolls back, and its redelivery then sees the committed row and skips.

### Saga Engine

Generic `SagaEngine<S, T>` with choreography semantics. On step failure or `failSaga`, an *after-commit* hook
(`TransactionSynchronizationManager`) delegates to `SagaCompensationRunner.runCompensation` in a `REQUIRES_NEW`
transaction (runs only if business tx commits).

### Saga Watchdog

A scheduled job (`@Scheduled`) that times out sagas stuck in `AWAITING_RESPONSE` and retries compensation for
`COMPENSATING` / `COMPENSATION_FAILED` states based on a cooldown.

### Compensation Topic

Follows the convention `SagaCompensationTopic.PREFIX + "<service>"` — each service composes its own neutral topic (e.g.
`saga-compensation-project`).

### Saga Log Correlation

Feedback events carry `sagaId`; the originating saga sits in `AWAITING_RESPONSE` until matched.

### Recovery Jobs

Service-local `SchedulingConfig` wires `@EnableScheduling` so `KafkaOutboxProcessor` and `SagaWatchdog` ticks fire.

---

## Example: Inviting Somebody to a Project

The saga that spans `project-service` and `mail-service`, by way of `notification-service`.

> [!IMPORTANT]
>
> Compensation only exists where effects are reversible. A sent e-mail cannot be un-sent, therefore `mail-service`
> has no compensation of its own.

### Phase 1 — Init (`project-service`)

Begins local saga `ProjectInvitation` and records `PersistInvitation`. The invitation row and the
`project-member-invited` event commit together; the event carries the saga id.

### Phase 2 — Publish (outbox poller)

Relays the Avro-serialized event to Kafka.

### Phase 3 — Relay (`notification-service`)

Claims the event through `ProcessedEventGuard`, raises the in-app notification, and asks for the mail. It runs no
saga of its own — it copies the saga id through to `mail-requested`, which is the only reason `project-service` can
recognize the answer later.

### Phase 4 — Process (`mail-service`)

Claims the command, begins local saga `EmailSending`, performs `ProcessTemplate` and `SendEmail`, completes the saga
and writes `mail-sent` (or `mail-failed`) to its outbox, carrying the same saga id back.

### Phase 5 — Feedback (`project-service`)

Consumes the feedback and matches it by saga id.

- **On `mail-sent`** — completes the saga.
- **On `mail-failed`** — fails it, and the after-commit hook runs `SagaCompensationRunner`, which publishes to
  `saga-compensation-project`; the invitation is expired rather than left pending on a mail nobody received.

Identity is not part of this: Keycloak owns registration, password reset and the mails that go with them, so no saga
spans them. See [Keycloak](./keycloak.md).

---

## Event-Driven Communication

Services communicate asynchronously through Kafka events. Integration events are defined as **Avro** schemas under
`contracts/<service>/<topic>/v<n>/*.avsc` and serialized in binary form with Schema Registry.

> [!NOTE]
>
> All publishing goes through the Outbox (`KafkaOutboxProcessor`); all consumption goes through `ProcessedEventGuard`
> for idempotency.

| Event                                 | Publisher              | Consumer          | Details                                                                                                                                                                                                |
|---------------------------------------|------------------------|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MailRequestedCommand`                | `notification-service` | `mail-service`    | A command, not a fact: the caller names the template and the recipient, and mail-service owns the wording. Its contract therefore belongs to the consumer.                                             |
| `MailSentEvent`<br/>`MailFailedEvent` | `mail-service`         | `project-service` | Published through the Transactional Outbox, carrying the saga id back so the originating saga can be advanced or failed.                                                                               |
| Compensation Actions                  | `project-service`      | `project-service` | Published to the internal `saga-compensation-project` topic as an Avro **tagged union**, decoded by an ACL translator into a typed `sealed interface` and handled by a compile-time exhaustive `when`. |
