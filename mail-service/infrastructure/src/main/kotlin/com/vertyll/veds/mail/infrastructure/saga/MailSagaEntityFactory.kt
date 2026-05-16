package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEntityFactory
import java.time.Instant

class MailSagaEntityFactory : SagaEntityFactory<SagaJpaEntity, SagaStepJpaEntity> {
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
