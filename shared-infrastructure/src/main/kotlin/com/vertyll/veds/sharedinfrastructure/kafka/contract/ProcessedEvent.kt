package com.vertyll.veds.sharedinfrastructure.kafka.contract

import java.time.Instant

/**
 * Persistence-agnostic record of an event already processed by a Kafka
 * consumer group. Backbone of the consumer-side **idempotent receiver**
 * pattern (Hohpe & Woolf, *Enterprise Integration Patterns*).
 *
 * The pair `(eventId, consumerGroup)` is the natural unique key — the same
 * event may legitimately be processed by multiple distinct consumer groups,
 * but never twice within the same group.
 */
interface ProcessedEvent {
    val id: Long?
    val eventId: String
    val consumerGroup: String
    val processedAt: Instant
}
