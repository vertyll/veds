package com.vertyll.veds.template.infrastructure.persistence.adapter

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import com.vertyll.veds.template.infrastructure.persistence.entity.OutboxJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.repository.OutboxJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * JPA-backed implementation of the outbox ports for the template-service.
 */
@Component
internal class OutboxJpaAdapter(
    private val repository: OutboxJpaRepository,
) : OutboxRepositoryPort,
    OutboxMessageFactory {
    override fun save(message: OutboxMessage): OutboxMessage {
        val entity = message as? OutboxJpaEntity ?: copyToJpaEntity(message)
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
        payload: ByteArray,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage =
        OutboxJpaEntity(
            topic = topic,
            key = key,
            payload = payload,
            sagaId = sagaId,
            eventId = eventId ?: UUID.randomUUID().toString(),
        )

    private fun copyToJpaEntity(message: OutboxMessage): OutboxJpaEntity =
        OutboxJpaEntity(
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
