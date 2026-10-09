# Sagas

How a workflow spanning services completes or is compensated, without a central orchestrator.

Generic `SagaEngine<S, T>` with choreography semantics. On step failure or `failSaga`, an *after-commit* hook
(`TransactionSynchronizationManager`) delegates to `SagaCompensationRunner.runCompensation` in a `REQUIRES_NEW`
transaction (runs only if business tx commits).

## Saga Watchdog

A scheduled job (`@Scheduled`) that times out sagas stuck in `AWAITING_RESPONSE` and retries compensation for
`COMPENSATING` / `COMPENSATION_FAILED` states based on a cooldown.

## Compensation Topic

Follows the convention `SagaCompensationTopic.PREFIX + "<service>"` — each service composes its own neutral topic (e.g.
`saga-compensation-project`).

## Saga Log Correlation

Feedback events carry `sagaId`; the originating saga sits in `AWAITING_RESPONSE` until matched.

## Recovery Jobs

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
spans them. See [Keycloak](../keycloak.md).

---
