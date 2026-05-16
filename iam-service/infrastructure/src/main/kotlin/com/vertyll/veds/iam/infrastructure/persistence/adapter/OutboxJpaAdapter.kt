package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.infrastructure.persistence.entity.OutboxJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.OutboxJpaRepository
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * JPA-backed adapter implementing the outbox ports for iam-service.
 */
@Component
class OutboxJpaAdapter(
    private val repository: OutboxJpaRepository,
) : OutboxRepositoryPort,
    OutboxMessageFactory {
    override fun save(message: OutboxMessage): OutboxMessage {
        val entity = message as? OutboxJpaEntity ?: copyToJpaEntity(message)
        return repository.save(entity)
    }

    override fun findByStatus(status: OutboxStatus): List<OutboxMessage> = repository.findByStatus(status)

    override fun findBySagaId(sagaId: String): List<OutboxMessage> = repository.findBySagaId(sagaId)

    override fun findByEventId(eventId: String): OutboxMessage? = repository.findByEventId(eventId)

    override fun lockBatchForDispatch(
        maxRetries: Int,
        retriableBefore: Instant,
        stuckBefore: Instant,
        batchSize: Int,
    ): List<OutboxMessage> =
        repository.lockBatchForDispatch(
            maxRetries = maxRetries,
            retriableBefore = retriableBefore,
            stuckBefore = stuckBefore,
            pageable = PageRequest.of(0, batchSize),
        )

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
