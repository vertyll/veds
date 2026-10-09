# Transactional outbox

How a state change and the event announcing it commit together, and how the event then reaches Kafka.

Consists of `KafkaOutboxProcessor` (poller) and `OutboxDispatchTx` (transactional helper).

- **Two-phase dispatch** — Claims a batch with `SELECT … FOR UPDATE SKIP LOCKED` (`READY → PROCESSING`). Kafka
  publication happens *outside* any DB transaction. Success/failure is recorded in a fresh `REQUIRES_NEW` transaction
  (`COMPLETED`, `markRetryScheduled`, `markDeadLettered`).
- **Idempotency** — UNIQUE constraint on `event_id`.
- **Reaper** — Rescues stuck `PROCESSING` rows abandoned by a crash.
- **Config** — Externalized via `KafkaOutboxProperties`.
