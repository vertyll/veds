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
    /** Storage-assigned surrogate id; `null` until the row is first persisted. */
    val id: Long?

    /** Inbound `eventId` Kafka header value being marked as processed. */
    val eventId: String

    /** Kafka consumer group that processed the event (one row per `(eventId, consumerGroup)`). */
    val consumerGroup: String

    /** Instant the row was inserted, i.e. when processing completed. */
    val processedAt: Instant
}
