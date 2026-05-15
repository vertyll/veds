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
abstract class BaseSagaStep(
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
) : SagaStep {
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    final override var status: SagaStepStatus = status
        private set

    @Column(nullable = true)
    final override var errorMessage: String? = errorMessage
        private set

    @Column(nullable = true)
    final override var completedAt: Instant? = completedAt
        private set

    @Column(nullable = true)
    final override var compensationStepId: Long? = compensationStepId
        private set

    override fun markCompleted(): SagaStep {
        status = SagaStepStatus.COMPLETED
        completedAt = Instant.now()
        return this
    }

    override fun markFailed(error: String): SagaStep {
        status = SagaStepStatus.FAILED
        errorMessage = error
        return this
    }

    override fun markCompensated(): SagaStep {
        status = SagaStepStatus.COMPENSATED
        return this
    }

    override fun markCompensationFailed(error: String?): SagaStep {
        status = SagaStepStatus.COMPENSATION_FAILED
        if (error != null) errorMessage = error
        return this
    }

    override fun linkToCompensationStep(compensationStepId: Long): SagaStep {
        this.compensationStepId = compensationStepId
        return this
    }
}
