package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

/**
 * Factory hook used by [SagaCompensationEngine] to create a service-specific
 * compensation step instance against the persistence-agnostic [SagaStep]
 * contract.
 */
@Suppress("kotlin:S6517")
interface SagaCompensationStepFactory<T : SagaStep> {
    fun createCompensationStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): T
}
