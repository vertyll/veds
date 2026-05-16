package com.vertyll.veds.template.application.port.out

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.template.application.saga.model.Saga
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.SagaTypes

interface SagaProcessPort {
    fun startSaga(
        sagaType: SagaTypes,
        payload: Map<String, Any?>,
    ): Saga

    fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Map<String, Any?> = emptyMap(),
    )

    fun markSagaCompleted(sagaId: String)

    fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    )

    fun markAwaitingResponse(sagaId: String)

    fun findSagaDomainById(sagaId: String): Saga?
}
