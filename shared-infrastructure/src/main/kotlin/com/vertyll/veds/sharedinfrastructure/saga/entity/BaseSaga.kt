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
abstract class BaseSaga<S : BaseSaga<S>>(
    @Id
    override var id: String,
    @Column(nullable = false)
    override var type: String,
    status: SagaStatus,
    @Column(nullable = false, columnDefinition = "TEXT")
    override var payload: String,
    lastError: String? = null,
    @Column(nullable = false)
    override var startedAt: Instant,
    completedAt: Instant? = null,
    updatedAt: Instant = Instant.now(),
    @Version
    override var version: Long? = null,
) : Saga<S> {
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

    /**
     * Concrete subclasses return `this` as the F-bounded self type [S].
     * Implementing as `override fun self() = this` is the canonical CRTP
     * (Curiously Recurring Template Pattern) hook that lets the base class
     * return the concrete subtype without any unchecked casts.
     */
    protected abstract fun self(): S

    override fun markCompleted(): S {
        val now = Instant.now()
        status = SagaStatus.COMPLETED
        completedAt = now
        updatedAt = now
        return self()
    }

    override fun markAwaitingResponse(): S {
        status = SagaStatus.AWAITING_RESPONSE
        updatedAt = Instant.now()
        return self()
    }

    override fun markFailed(error: String): S {
        val now = Instant.now()
        status = SagaStatus.FAILED
        lastError = error
        completedAt = now
        updatedAt = now
        return self()
    }

    override fun startCompensating(error: String): S {
        status = SagaStatus.COMPENSATING
        lastError = error
        updatedAt = Instant.now()
        return self()
    }

    override fun markCompensated(): S {
        val now = Instant.now()
        status = SagaStatus.COMPENSATED
        completedAt = now
        updatedAt = now
        return self()
    }

    override fun markCompensationFailed(): S {
        val now = Instant.now()
        status = SagaStatus.COMPENSATION_FAILED
        completedAt = now
        updatedAt = now
        return self()
    }
}
