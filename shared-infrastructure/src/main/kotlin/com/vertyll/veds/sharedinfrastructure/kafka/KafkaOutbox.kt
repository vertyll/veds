package com.vertyll.veds.sharedinfrastructure.kafka

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

/**
 * Entity representing a message to be sent to Kafka.
 * Implements the Outbox Pattern for reliable event publishing in distributed transactions.
 */
@Entity
@Table(name = "kafka_outbox")
class KafkaOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var eventId: String = UUID.randomUUID().toString(),
    @Column(nullable = false)
    val topic: String,
    @Column(nullable = false)
    val key: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OutboxStatus = OutboxStatus.PENDING,
    @Column(nullable = true)
    var errorMessage: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = true)
    var processedAt: Instant? = null,
    @Column(nullable = false)
    var retryCount: Int = 0,
    @Column(nullable = true)
    var lastRetryAt: Instant? = null,
    @Column(nullable = true)
    var sagaId: String? = null,
    @Version
    val version: Long? = null,
) {
    constructor() : this(
        id = null,
        topic = "",
        key = "",
        payload = "",
        status = OutboxStatus.PENDING,
        errorMessage = null,
        processedAt = null,
        retryCount = 0,
        lastRetryAt = null,
        sagaId = null,
        version = null,
    )

    enum class OutboxStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
    }
}
