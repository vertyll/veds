package com.vertyll.veds.sharedinfrastructure.kafka.contract

import java.time.Instant

/**
 * Persistence-agnostic contract for a Kafka outbox message.
 *
 * Concrete implementations may be JPA entities, Mongo documents, in-memory
 * data classes, etc. The processor consumes this interface only.
 */
interface OutboxMessage {
    val id: Long?
    var eventId: String
    val topic: String
    val key: String
    val payload: String
    var status: OutboxStatus
    var errorMessage: String?
    val createdAt: Instant
    var processedAt: Instant?
    var retryCount: Int
    var lastRetryAt: Instant?
    var sagaId: String?
    val version: Long?
}
