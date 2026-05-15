package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

/**
 * Factory hook used by [SagaCompensationEngine] to create a service-specific
 * compensation step entity. Replaces the abstract `createCompensationStepEntity`
 * method previously found on `BaseSagaCompensationService` (Template Method)
 * with composition.
 */
@Suppress("kotlin:S6517")
interface SagaCompensationStepFactory<T : BaseSagaStep> {
    fun createCompensationStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): T
}
