package com.vertyll.veds.sharedinfrastructure.kafka.contract

/**
 * Persistence-agnostic factory that produces a new [OutboxMessage] instance
 * matching the storage backend in use (JPA entity, Mongo document,
 * Cassandra row, …).
 */
@Suppress("kotlin:S6517")
interface OutboxMessageFactory {
    fun create(
        topic: String,
        key: String,
        payload: ByteArray,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage
}
