package com.vertyll.veds.sharedinfrastructure.kafka.contract

import java.time.Instant

/**
 * Persistence-agnostic contract for a Kafka outbox message.
 *
 * The contract is **immutable from the caller's perspective**: all properties
 * are `val` and every state transition is expressed as a domain method that
 * returns the new message instance (e.g. [markProcessing], [markCompleted],
 * [markFailed]). Concrete adapters decide whether to mutate the underlying
 * row in place (JPA dirty-tracking with `var` private-set fields) or return a
 * freshly copied document (MongoDB, Cassandra). The outbox processor never
 * observes the difference — it always works with the returned reference.
 */
interface OutboxMessage {
    val id: Long?
    val eventId: String
    val topic: String
    val key: String
    val payload: ByteArray
    val status: OutboxStatus
    val errorMessage: String?
    val createdAt: Instant
    val processedAt: Instant?
    val retryCount: Int
    val lastRetryAt: Instant?
    val sagaId: String?
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
     * Marks the message as failed after a publishing attempt. Sets [status]
     * to [OutboxStatus.FAILED], records [errorMessage], increments
     * [retryCount] and stamps [lastRetryAt] with the current instant.
     */
    fun markFailed(error: String): OutboxMessage
}
