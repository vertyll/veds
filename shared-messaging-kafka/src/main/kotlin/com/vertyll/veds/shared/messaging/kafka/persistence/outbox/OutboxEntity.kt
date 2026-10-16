@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.shared.messaging.kafka.persistence.outbox

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessage
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A row of the transactional outbox, written in the same transaction as the
 * business change it announces.
 *
 * Every service maps this one entity onto its own `kafka_outbox` table, created
 * by its own Flyway migration; the table is per-database, the mapping is not. A
 * service enables it by naming this package in `@EntityScan`, which is also how
 * a service without an outbox — one that publishes nothing — leaves it out.
 *
 * Mutable fields use `var` with `protected set` so Hibernate dirty-tracking can
 * emit partial `UPDATE` statements while callers see an immutable API and reach
 * the state through the `mark*` behavior methods. A different store implements
 * [OutboxMessage] directly and returns fresh copies instead; the processor never
 * observes the difference.
 */
@Entity
@Table(name = "kafka_outbox")
class OutboxEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null,
    eventId: String = Uuid.generateV7().toString(),
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
    @Column(nullable = false, unique = true)
    override var eventId: String = eventId
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    override var status: OutboxStatus = status
        protected set

    @Column(nullable = true)
    override var errorMessage: String? = errorMessage
        protected set

    @Column(nullable = true)
    override var processedAt: Instant? = processedAt
        protected set

    @Column(nullable = false)
    override var retryCount: Int = retryCount
        protected set

    @Column(nullable = true)
    override var lastRetryAt: Instant? = lastRetryAt
        protected set

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

    override fun markRetryScheduled(error: String): OutboxMessage {
        status = OutboxStatus.PENDING
        errorMessage = error
        retryCount += 1
        lastRetryAt = Instant.now()
        return this
    }

    override fun markDeadLettered(error: String): OutboxMessage {
        status = OutboxStatus.DEAD_LETTERED
        errorMessage = error
        lastRetryAt = Instant.now()
        return this
    }
}
