package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.SagaCompensationStepFactory
import java.time.Instant

internal class ProjectSagaCompensationStepFactory : SagaCompensationStepFactory<SagaStepJpaEntity> {
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
