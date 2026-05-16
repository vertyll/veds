package com.vertyll.veds.sharedinfrastructure.kafka.entity

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

/**
 * JPA `@MappedSuperclass` providing the column mapping for a Kafka outbox
 * row.
 *
 * Concrete per-service entities (e.g. `OutboxJpaEntity` in iam/mail/template)
 * extend this class with their own `@Entity` + `@Table(name = "kafka_outbox")`
 * annotations. The base class implements the persistence-agnostic
 * [OutboxMessage] contract: mutable fields use `var` with `private set` so
 * that Hibernate dirty-tracking can emit partial `UPDATE` statements while
 * still exposing an effectively-immutable API to callers via behavior
 * methods (DDD rich aggregate).
 *
 * Mongo/Cassandra adapters can implement these methods by returning a fresh
 * copy of the document instead. The processor never observes the
 * difference.
 */
@MappedSuperclass
abstract class BaseOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null,
    eventId: String = UUID.randomUUID().toString(),
    @Column(nullable = false)
    override var topic: String,
    @Column(nullable = false)
    override var key: String,
    @Column(nullable = false, columnDefinition = "BYTEA")
    override var payload: ByteArray,
    status: OutboxStatus = OutboxStatus.PENDING,
    errorMessage: String? = null,
    @Column(nullable = false)
    override var createdAt: Instant = Instant.now(),
    processedAt: Instant? = null,
    retryCount: Int = 0,
    lastRetryAt: Instant? = null,
    @Column(nullable = true)
    override var sagaId: String? = null,
    @Version
    override var version: Long? = null,
) : OutboxMessage {
    @Column(nullable = false)
    final override var eventId: String = eventId
        private set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    final override var status: OutboxStatus = status
        private set

    @Column(nullable = true)
    final override var errorMessage: String? = errorMessage
        private set

    @Column(nullable = true)
    final override var processedAt: Instant? = processedAt
        private set

    @Column(nullable = false)
    final override var retryCount: Int = retryCount
        private set

    @Column(nullable = true)
    final override var lastRetryAt: Instant? = lastRetryAt
        private set

    override fun markProcessing(): OutboxMessage {
        status = OutboxStatus.PROCESSING
        processedAt = Instant.now()
        return this
    }

    override fun markCompleted(): OutboxMessage {
        status = OutboxStatus.COMPLETED
        processedAt = Instant.now()
        return this
    }

    override fun markFailed(error: String): OutboxMessage {
        status = OutboxStatus.FAILED
        errorMessage = error
        retryCount += 1
        lastRetryAt = Instant.now()
        return this
    }
}
