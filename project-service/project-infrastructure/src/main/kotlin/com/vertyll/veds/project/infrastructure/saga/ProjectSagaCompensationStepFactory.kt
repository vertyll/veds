package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.saga.engine.CompensationStepDraft
import com.vertyll.veds.shared.saga.engine.SagaCompensationStepFactory

internal class ProjectSagaCompensationStepFactory : SagaCompensationStepFactory<SagaStepJpaEntity> {
    override fun createCompensationStep(draft: CompensationStepDraft): SagaStepJpaEntity =
        SagaStepJpaEntity(
            sagaId = draft.sagaId,
            stepName = draft.stepName,
            status = draft.status,
            createdAt = draft.createdAt,
            completedAt = draft.completedAt,
            compensationStepId = draft.compensationStepId,
        )
}
