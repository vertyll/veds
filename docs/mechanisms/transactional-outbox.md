# Transactional outbox

How a state change and the event announcing it commit together, and how the event then reaches Kafka.

Consists of `KafkaOutboxProcessor` (poller) and `OutboxDispatchTx` (transactional helper).

- **Two-phase dispatch** — Claims a batch with `SELECT … FOR UPDATE SKIP LOCKED` (`READY → PROCESSING`). Kafka
  publication happens *outside* any DB transaction. Success/failure is recorded in a fresh `REQUIRES_NEW` transaction
  (`COMPLETED`, `markRetryScheduled`, `markDeadLettered`).
- **Order per key** — The claim skips a row while an older row with the same topic and key is `PROCESSING`, or
  `PENDING` after a failed attempt, so the events about one aggregate reach Kafka in the order they were written. When a
  send fails, the rest of the batch's rows with that topic and key go back to `PENDING` unsent (`markReleased`), with no
  attempt counted. A dead-lettered row holds nothing back. A partial index,
  `idx_kafka_outbox_topic_key_id_unsent`, keeps the guard cheap.
- **Bounded send** — The poller waits at most `send-timeout` (45 s) for the broker's acknowledgment, and the producer
  itself gives up sooner: `max.block.ms` 10 s, `delivery.timeout.ms` 30 s. Both stay well below `stuck-threshold`
  (5 min), so a row is never reclaimed while its send is still pending.
- **Idempotency** — UNIQUE constraint on `event_id`.
- **Reaper** — Rescues stuck `PROCESSING` rows abandoned by a crash.
- **Config** — Externalized via `KafkaOutboxProperties` (`application.outbox.*`), validated at start-up; the poller
  runs with a fixed delay of `poll-interval` (5 s).

> [!NOTE]
>
> The order per key holds for one poller. Each service runs one replica; with several, `SKIP LOCKED` lets a second
> replica claim a newer row while the first holds an older one with the same key. Running more than one replica needs
> a lease, or rows sharded by key, first.
