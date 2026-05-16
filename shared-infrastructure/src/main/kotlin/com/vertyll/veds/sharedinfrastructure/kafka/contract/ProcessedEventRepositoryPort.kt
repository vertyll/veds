package com.vertyll.veds.sharedinfrastructure.kafka.contract

import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard

/**
 * Persistence-agnostic repository port for [ProcessedEvent] rows.
 *
 * Adapters back this port with the storage technology of choice
 * (JPA today, Mongo/Cassandra possible). Implementations MUST honor the
 * uniqueness of `(eventId, consumerGroup)` — duplicate inserts within the
 * same group must throw so the [ProcessedEventGuard]
 * can treat them as already-processed.
 */
interface ProcessedEventRepositoryPort {
    /**
     * Inserts a new processed-event row. Throws if a row with the same
     * `(eventId, consumerGroup)` already exists.
     */
    fun insert(processedEvent: ProcessedEvent): ProcessedEvent

    /**
     * Returns `true` when a row already exists for the
     * `(eventId, consumerGroup)` pair. Primarily useful for diagnostics —
     * the [ProcessedEventGuard]
     * relies on the UNIQUE-constraint violation from [insert] for the
     * race-free idempotency check.
     */
    fun exists(
        eventId: String,
        consumerGroup: String,
    ): Boolean
}
