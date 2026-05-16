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

    fun exists(
        eventId: String,
        consumerGroup: String,
    ): Boolean
}
