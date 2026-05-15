package com.vertyll.veds.mail.infrastructure.persistence.entity

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import com.vertyll.veds.sharedinfrastructure.kafka.entity.BaseOutbox
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "kafka_outbox")
internal class OutboxJpaEntity(
    id: Long? = null,
    eventId: String = UUID.randomUUID().toString(),
    topic: String,
    key: String,
    payload: String,
    status: OutboxStatus = OutboxStatus.PENDING,
    errorMessage: String? = null,
    createdAt: Instant = Instant.now(),
    processedAt: Instant? = null,
    retryCount: Int = 0,
    lastRetryAt: Instant? = null,
    sagaId: String? = null,
    version: Long? = null,
) : BaseOutbox(
        id = id,
        eventId = eventId,
        topic = topic,
        key = key,
        payload = payload,
        status = status,
        errorMessage = errorMessage,
        createdAt = createdAt,
        processedAt = processedAt,
        retryCount = retryCount,
        lastRetryAt = lastRetryAt,
        sagaId = sagaId,
        version = version,
    )
