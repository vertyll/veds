package com.vertyll.veds.sharedinfrastructure.kafka.contract

/**
 * Persistence-agnostic factory that produces a new [OutboxMessage] instance
 * matching the storage backend in use (JPA entity, Mongo document,
 * Cassandra row, …).
 */
@Suppress("kotlin:S6517")
interface OutboxMessageFactory {
    /**
     * Builds a new outbox message in [OutboxStatus.PENDING].
     *
     * @param topic Kafka destination topic.
     * @param key Kafka record key (partitioning).
     * @param payload Pre-serialized record value.
     * @param sagaId Optional saga correlation id.
     * @param eventId Optional caller-supplied event id; implementations generate one when `null`.
     */
    fun create(
        topic: String,
        key: String,
        payload: ByteArray,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage
}
