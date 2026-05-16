package com.vertyll.veds.sharedinfrastructure.saga.entity

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant

/**
 * JPA-backed base implementation of the [SagaStep] port. See [BaseSaga] for
 * the rationale behind the contract/adapter split — the engine sees an
 * immutable port; this class mutates `var` fields internally and returns
 * `this` so Hibernate can dirty-track the update.
 */
@MappedSuperclass
abstract class BaseSagaStep<T : BaseSagaStep<T>>(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null,
    @Column(nullable = false)
    override var sagaId: String,
    @Column(nullable = false)
    override var stepName: String,
    status: SagaStepStatus,
    @Column(nullable = true, columnDefinition = "TEXT")
    override var payload: String? = null,
    errorMessage: String? = null,
    @Column(nullable = false)
    override var createdAt: Instant,
    completedAt: Instant? = null,
    compensationStepId: Long? = null,
    @Version
    override var version: Long? = null,
) : SagaStep<T> {
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    override var status: SagaStepStatus = status
        protected set

    @Column(nullable = true)
    override var errorMessage: String? = errorMessage
        protected set

    @Column(nullable = true)
    override var completedAt: Instant? = completedAt
        protected set

    @Column(nullable = true)
    override var compensationStepId: Long? = compensationStepId
        protected set

    /** See [BaseSaga.self] — CRTP hook returning the concrete subtype. */
    protected abstract fun self(): T

    override fun markCompleted(): T {
        status = SagaStepStatus.COMPLETED
        completedAt = Instant.now()
        return self()
    }

    override fun markFailed(error: String): T {
        status = SagaStepStatus.FAILED
        errorMessage = error
        return self()
    }

    override fun markCompensated(): T {
        status = SagaStepStatus.COMPENSATED
        return self()
    }

    override fun markCompensationFailed(error: String?): T {
        status = SagaStepStatus.COMPENSATION_FAILED
        if (error != null) errorMessage = error
        return self()
    }

    override fun linkToCompensationStep(compensationStepId: Long): T {
        this.compensationStepId = compensationStepId
        return self()
    }
}
