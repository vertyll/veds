package com.vertyll.veds.sharedinfrastructure.kafka.contract

/**
 * Persistence-agnostic factory for [ProcessedEvent] rows, owned by each
 * service's adapter (analogous to [OutboxMessageFactory]).
 */
@Suppress("kotlin:S6517")
interface ProcessedEventFactory {
    fun create(
        eventId: String,
        consumerGroup: String,
    ): ProcessedEvent
}
