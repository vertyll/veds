package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.port.out.SagaProcessPort
import com.vertyll.veds.mail.application.saga.model.Saga
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.application.saga.model.SagaTypes
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaEngine
import org.springframework.stereotype.Service

@Service
internal class SagaManagerAdapter(
    private val sagaEngine: SagaEngine<SagaJpaEntity, *>,
) : SagaProcessPort {
    override fun startSaga(
        sagaType: SagaTypes,
        payload: Map<String, Any?>,
    ): Saga = sagaEngine.startSaga(sagaType = sagaType, payload = payload).toSagaDomain()

    override fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        sagaEngine.recordSagaStep(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
        )
    }

    override fun markSagaCompleted(sagaId: String) {
        sagaEngine.completeSaga(sagaId)
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        sagaEngine.failSaga(sagaId, errorMessage)
    }

    override fun markAwaitingResponse(sagaId: String) {
        sagaEngine.awaitResponse(sagaId)
    }

    override fun findSagaDomainById(sagaId: String): Saga? = sagaEngine.findSagaById(sagaId)?.toSagaDomain()
}

private fun SagaJpaEntity.toSagaDomain(): Saga =
    Saga(
        id = this.id,
        type = this.type,
        status = this.status,
        payload = this.payload,
        lastError = this.lastError,
        startedAt = this.startedAt,
        completedAt = this.completedAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
