package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.SagaEntityFactory
import java.time.Instant

internal class ProjectSagaEntityFactory : SagaEntityFactory<SagaJpaEntity, SagaStepJpaEntity> {
    override fun createSaga(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): SagaJpaEntity =
        SagaJpaEntity(
            id = id,
            type = type,
            status = status,
            payload = payload,
            startedAt = startedAt,
        )

    override fun createSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): SagaStepJpaEntity =
        SagaStepJpaEntity(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
            createdAt = createdAt,
        )
}
