package com.vertyll.veds.mail.application.saga.port

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus

interface SagaProcessPort {
    fun startSaga(
        sagaType: String,
        payload: Map<String, Any?>,
    ): String

    fun recordSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    )

    fun markSagaCompleted(sagaId: String)
}
