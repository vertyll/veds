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

## Building blocks

All of them live in `shared-messaging-kafka` and `shared-saga-engine`:

- [Transactional outbox](mechanisms/transactional-outbox.md) – a state change and the event announcing it commit
  together, and a poller publishes it.
- [Inbox](mechanisms/inbox.md) – a delivery is handled once, and a failed handler leaves nothing claimed.
- [Sagas](mechanisms/sagas.md) – the local saga engine, the watchdog, compensation topics, log correlation, recovery
  jobs, and one complete run: inviting somebody to a project.

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
