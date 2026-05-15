package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import java.time.Instant
import java.util.UUID

/**
 * JPA implementation of the [OutboxRepositoryPort] and [OutboxMessageFactory]
 * ports. Delegates to the Spring Data JPA repository [KafkaOutboxRepository].
 *
 * Registered as a Spring bean by [JpaOutboxAdapterAutoConfiguration] only
 * when no other [OutboxRepositoryPort] / [OutboxMessageFactory] bean has been
 * provided by the application — services backed by a different storage
 * technology (MongoDB, Cassandra, …) supply their own port bean and this
 * adapter steps aside.
 */
internal class KafkaOutboxJpaAdapter(
    private val repository: KafkaOutboxRepository,
) : OutboxRepositoryPort,
    OutboxMessageFactory {
    override fun save(message: OutboxMessage): OutboxMessage {
        val entity = message as? KafkaOutbox ?: copyToJpaEntity(message)
        return repository.save(entity)
    }

    override fun findByStatus(status: OutboxStatus): List<OutboxMessage> = repository.findByStatus(status)

    override fun findBySagaId(sagaId: String): List<OutboxMessage> = repository.findBySagaId(sagaId)

    override fun findMessagesToProcess(
        status: OutboxStatus,
        maxRetries: Int,
        minRetryTime: Instant,
    ): List<OutboxMessage> = repository.findMessagesToProcess(status, maxRetries, minRetryTime)

    override fun create(
        topic: String,
        key: String,
        payload: String,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage =
        KafkaOutbox(
            topic = topic,
            key = key,
            payload = payload,
            sagaId = sagaId,
            eventId = eventId ?: UUID.randomUUID().toString(),
        )

    private fun copyToJpaEntity(message: OutboxMessage): KafkaOutbox =
        KafkaOutbox(
            id = message.id,
            eventId = message.eventId,
            topic = message.topic,
            key = message.key,
            payload = message.payload,
            status = message.status,
            errorMessage = message.errorMessage,
            createdAt = message.createdAt,
            processedAt = message.processedAt,
            retryCount = message.retryCount,
            lastRetryAt = message.lastRetryAt,
            sagaId = message.sagaId,
            version = message.version,
        )
}
