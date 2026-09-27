package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.SagaStepStatus
import java.time.Instant

internal data class TestStep(
    override val id: Long? = null,
    override val sagaId: String,
    override val stepName: String,
    override val status: SagaStepStatus,
    override val payload: String? = null,
    override val errorMessage: String? = null,
    override val createdAt: Instant = Instant.now(),
    override val completedAt: Instant? = null,
    override val compensationStepId: Long? = null,
    override val version: Long? = null,
) : SagaStep<TestStep> {
    override fun markCompleted() = copy(status = SagaStepStatus.COMPLETED, completedAt = Instant.now())

    override fun markFailed(error: String) = copy(status = SagaStepStatus.FAILED, errorMessage = error)

    override fun markCompensated() = copy(status = SagaStepStatus.COMPENSATED, completedAt = Instant.now())

    override fun markCompensationFailed(error: String?) = copy(status = SagaStepStatus.COMPENSATION_FAILED, errorMessage = error)

    override fun linkToCompensationStep(compensationStepId: Long) = copy(compensationStepId = compensationStepId)
}
