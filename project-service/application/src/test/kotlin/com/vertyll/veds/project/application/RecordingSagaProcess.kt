package com.vertyll.veds.project.application

import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaSnapshot
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.SagaTypeValue

internal class RecordingSagaProcess : SagaProcessPort {
    val trail = mutableListOf<String>()
    var started: SagaSnapshot? = null

    override fun startSaga(
        sagaType: SagaTypeValue,
        payload: Map<String, Any?>,
    ): SagaSnapshot =
        SagaSnapshot(id = "saga-1", type = sagaType.value, status = SagaStatus.STARTED, payload = payload.toString())
            .also {
                started = it
                trail += "start(${sagaType.value})"
            }

    override fun recordSagaStep(
        sagaId: String,
        stepName: SagaTypeValue,
        status: SagaStepStatus,
        payload: Map<String, Any?>,
    ) {
        trail += "step(${stepName.value},$status)"
    }

    override fun markSagaCompleted(sagaId: String) {
        trail += "completed"
    }

    override fun markSagaFailed(
        sagaId: String,
        errorMessage: String,
    ) {
        trail += "failed($errorMessage)"
    }

    override fun markAwaitingResponse(sagaId: String) {
        trail += "awaiting"
    }

    override fun findSagaById(sagaId: String) = started
}
