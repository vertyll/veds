package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
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
 * JPA-backed implementation of the [OutboxMessage] port.
 *
 * Other persistence flavors (Mongo, in-memory, …) can be added as parallel
 * implementations of [OutboxMessage] without touching the outbox processor.
 */
@Entity
@Table(name = "kafka_outbox")
class KafkaOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long? = null,
    @Column(nullable = false)
    override var eventId: String = UUID.randomUUID().toString(),
    @Column(nullable = false)
    override val topic: String,
    @Column(nullable = false)
    override val key: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    override val payload: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    override var status: OutboxStatus = OutboxStatus.PENDING,
    @Column(nullable = true)
    override var errorMessage: String? = null,
    @Column(nullable = false)
    override val createdAt: Instant = Instant.now(),
    @Column(nullable = true)
    override var processedAt: Instant? = null,
    @Column(nullable = false)
    override var retryCount: Int = 0,
    @Column(nullable = true)
    override var lastRetryAt: Instant? = null,
    @Column(nullable = true)
    override var sagaId: String? = null,
    @Version
    override val version: Long? = null,
) : OutboxMessage
