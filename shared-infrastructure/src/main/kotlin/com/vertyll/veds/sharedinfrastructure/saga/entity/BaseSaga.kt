package com.vertyll.veds.sharedinfrastructure.saga.entity

import com.vertyll.veds.sharedinfrastructure.saga.contract.Saga
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant

/**
 * JPA-backed base implementation of the [Saga] port.
 *
 * The [Saga] port exposes its fields as `val` (immutable from the engine's
 * point of view). Internally this class uses `var` because Jakarta
 * Persistence requires writable fields for proxy/dirty-tracking. State
 * transitions are exclusively performed via behavior methods (DDD rich
 * aggregate) which mutate the same instance and return `this` — Hibernate
 * then issues a partial UPDATE on flush.
 *
 * Mongo/Cassandra adapters can implement these methods by returning a fresh
 * copy of the document instead. The engine never observes this difference.
 */
@MappedSuperclass
abstract class BaseSaga(
    @Id
    override val id: String,
    @Column(nullable = false)
    override val type: String,
    status: SagaStatus,
    @Column(nullable = false, columnDefinition = "TEXT")
    override val payload: String,
    lastError: String? = null,
    @Column(nullable = false)
    override val startedAt: Instant,
    completedAt: Instant? = null,
    updatedAt: Instant = Instant.now(),
    @Version
    override val version: Long? = null,
) : Saga {
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    final override var status: SagaStatus = status
        private set

    @Column(nullable = true)
    final override var lastError: String? = lastError
        private set

    @Column(nullable = true)
    final override var completedAt: Instant? = completedAt
        private set

    @Column(nullable = false)
    final override var updatedAt: Instant = updatedAt
        private set

    override fun markCompleted(): Saga {
        val now = Instant.now()
        status = SagaStatus.COMPLETED
        completedAt = now
        updatedAt = now
        return this
    }

    override fun markAwaitingResponse(): Saga {
        status = SagaStatus.AWAITING_RESPONSE
        updatedAt = Instant.now()
        return this
    }

    override fun markFailed(error: String): Saga {
        val now = Instant.now()
        status = SagaStatus.FAILED
        lastError = error
        completedAt = now
        updatedAt = now
        return this
    }

    override fun startCompensating(error: String): Saga {
        status = SagaStatus.COMPENSATING
        lastError = error
        updatedAt = Instant.now()
        return this
    }

    override fun markCompensated(): Saga {
        val now = Instant.now()
        status = SagaStatus.COMPENSATED
        completedAt = now
        updatedAt = now
        return this
    }

    override fun markCompensationFailed(): Saga {
        val now = Instant.now()
        status = SagaStatus.COMPENSATION_FAILED
        completedAt = now
        updatedAt = now
        return this
    }
}
