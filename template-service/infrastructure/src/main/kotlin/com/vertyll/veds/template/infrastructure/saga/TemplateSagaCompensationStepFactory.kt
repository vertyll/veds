package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationStepFactory
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import java.time.Instant

internal class TemplateSagaCompensationStepFactory : SagaCompensationStepFactory<SagaStepJpaEntity> {
    override fun createCompensationStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): SagaStepJpaEntity =
        SagaStepJpaEntity(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            createdAt = createdAt,
            completedAt = completedAt,
            compensationStepId = compensationStepId,
        )
}
