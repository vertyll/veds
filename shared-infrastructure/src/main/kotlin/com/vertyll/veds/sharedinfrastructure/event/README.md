# Events in `shared-infrastructure`

This package intentionally exposes **no `DomainEvent` / `IntegrationEvent`
marker interface** and **no event classes**. Only a minimal helper [`Events`]
with `newId()` and `now()` lives here.

## Event taxonomy in this monorepo

| Category                                                                                      | Where it lives                                                                                                               | Carrier                                                 |
|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| **Integration events** – cross-service contracts (e.g. `MailRequestedEvent`, `MailSentEvent`) | `contracts/<service>/<topic>/v<n>/*.avsc` → generated `SpecificRecord` in each consumer/producer                             | Kafka topic (Avro + Schema Registry, binary)            |
| **Saga compensation events** – per-service contract for choreography rollback                 | `contracts/<service>/saga-compensation/v1/saga-compensation.avsc`                                                            | Kafka topic `saga-compensation-<service>`               |
| **Outbox messages** – internal transactional record                                           | `shared-infrastructure/.../kafka/entity/BaseOutbox.kt` + per-service `OutboxJpaEntity`                                       | PostgreSQL → relayed to Kafka by `KafkaOutboxProcessor` |
| **Domain events** (true DDD sense, in-process)                                                | _Not used yet._ Would live inside each service's `domain/` package and be dispatched via Spring `ApplicationEventPublisher`. | JVM in-process only                                     |
| **Application events** (Spring lifecycle, `@EventListener`)                                   | Per-service `infrastructure/config` if/when needed                                                                           | Spring `ApplicationEventPublisher`                      |

## Why no shared marker interface?

1. **Integration events are Avro-generated** Java classes. We don't own their
   superclass; trying to force them to implement a Kotlin interface would
   require either a build-time post-processor or handwritten wrappers — both
   defeat the point of code generation.
2. **`shared-infrastructure` must not know any business event** (no
   `MailRequestedEvent` here). The previous `DomainEvent` interface with
   `@JsonTypeInfo` was Jackson polymorphism for the old JSON-based outbox —
   completely irrelevant after the Avro migration.
3. **Outbox payloads are opaque bytes** (`ByteArray`) framed by Confluent's
   wire format. No type-level marker would make sense at the outbox layer.

## If you ever need real in-process domain events

Add them **inside the owning service**, e.g.:

```
iam-service/
  src/main/kotlin/com/vertyll/veds/iam/domain/event/
    UserRegistered.kt          // sealed interface or data class
    EmailChanged.kt
```

And publish via `ApplicationEventPublisher` (Spring), not Kafka. Keep them
**local** — when the event needs to leave the bounded context, model it as a
new Avro contract under `contracts/` instead.
