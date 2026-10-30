package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.shared.saga.engine.CompensationStepDraft
import com.vertyll.veds.shared.saga.engine.SagaCompensationStepFactory
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity

internal class TemplateSagaCompensationStepFactory : SagaCompensationStepFactory<SagaStepJpaEntity> {
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
