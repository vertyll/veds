package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import java.time.Instant

internal class TestEntityFactory : SagaEntityFactory<TestSaga, TestStep> {
    override fun createSaga(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ) = TestSaga(id = id, type = type, status = status, payload = payload, startedAt = startedAt)

    override fun createSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ) = TestStep(sagaId = sagaId, stepName = stepName, status = status, payload = payload, createdAt = createdAt)
}
