package com.vertyll.veds.sharedinfrastructure.kafka.contract

import java.time.Instant

/**
 * Persistence-agnostic contract for a Kafka outbox message.
 *
 * The contract is **immutable from the caller's perspective**: all properties
 * are `val` and every state transition is expressed as a domain method that
 * returns the new message instance (e.g. [markProcessing], [markCompleted],
 * [markRetryScheduled], [markDeadLettered]). Concrete adapters decide whether
 * to mutate the underlying row in place (JPA dirty-tracking with `var`
 * private-set fields) or return a freshly copied document (MongoDB,
 * Cassandra). The outbox processor never observes the difference — it always
 * works with the returned reference.
 */
interface OutboxMessage {
    /** Storage-assigned surrogate id; `null` until the row is first persisted. */
    val id: Long?

    /** Stable business identifier propagated as the `eventId` Kafka header (idempotency key on the consumer side). */
    val eventId: String

    /** Target Kafka topic name. */
    val topic: String

    /** Kafka record key used for partitioning (typically the aggregate id or saga id). */
    val key: String

    /** Serialized record value; encoding (Avro, JSON, …) is the caller's responsibility. */
    val payload: ByteArray

    /** Current lifecycle [OutboxStatus]. See the enum's KDoc for the state machine. */
    val status: OutboxStatus

    /** Latest publishing error message, or `null` if the message has never failed. */
    val errorMessage: String?

    /** Instant the row was first written by the producing transaction. */
    val createdAt: Instant

    /** Instant of the most recent dispatch attempt (success or failure). */
    val processedAt: Instant?

    /** Number of publishing attempts already made. Compared against `veds.outbox.max-retries`. */
    val retryCount: Int

    /** Instant of the most recent retry; used together with `veds.outbox.retry-cooldown` to throttle redelivery. */
    val lastRetryAt: Instant?

    /** Optional saga correlation id (`Saga.id`) so saga-related outbox rows can be located quickly. */
    val sagaId: String?

    /** JPA optimistic-locking version, or `null` for storage backends that do not provide one. */
    val version: Long?

    /**
     * Marks the message as being currently processed. Sets [status] to
     * [OutboxStatus.PROCESSING] and stamps [processedAt] with the current
     * instant.
     */
    fun markProcessing(): OutboxMessage

    /**
     * Marks the message as successfully published to the broker. Sets
     * [status] to [OutboxStatus.COMPLETED] and stamps [processedAt] with the
     * current instant.
     */
    fun markCompleted(): OutboxMessage

    /**
     * Reverts the message to [OutboxStatus.PENDING] after a failed publishing
     * attempt, incrementing [retryCount] and stamping [lastRetryAt]. The
     * message will be picked up again by the next poller cycle after the
     * configured retry cooldown elapses.
     */
    fun markRetryScheduled(error: String): OutboxMessage

    /**
     * Transitions the message to [OutboxStatus.DEAD_LETTERED]. Terminal —
     * the message will never be retried automatically.
     */
    fun markDeadLettered(error: String): OutboxMessage
}
