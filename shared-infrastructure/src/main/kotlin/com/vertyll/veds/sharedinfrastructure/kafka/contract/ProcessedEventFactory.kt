package com.vertyll.veds.sharedinfrastructure.kafka.contract

/**
 * Persistence-agnostic factory for [ProcessedEvent] rows, owned by each
 * service's adapter (analogous to [OutboxMessageFactory]).
 */
@Suppress("kotlin:S6517")
interface ProcessedEventFactory {
    /**
     * Builds a fresh [ProcessedEvent] for the given [eventId] / [consumerGroup]
     * pair. Implementations stamp `processedAt` with the current instant.
     */
    fun create(
        eventId: String,
        consumerGroup: String,
    ): ProcessedEvent
}
