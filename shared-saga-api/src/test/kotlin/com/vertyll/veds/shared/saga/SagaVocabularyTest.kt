package com.vertyll.veds.shared.saga

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SagaVocabularyTest {
    private enum class Step(
        override val value: String,
    ) : SagaTypeValue {
        SHIP("Ship"),
    }

    private class RecordingSagaProcess : SagaProcessPort {
        val payloads = mutableListOf<Map<String, Any?>>()

        override fun startSaga(
            sagaType: SagaTypeValue,
            payload: Map<String, Any?>,
        ) = SagaSnapshot(id = "saga", type = sagaType.value, status = SagaStatus.STARTED, payload = "{}")

        override fun recordSagaStep(
            sagaId: String,
            stepName: SagaTypeValue,
            status: SagaStepStatus,
            payload: Map<String, Any?>,
        ) {
            payloads += payload
        }

        override fun markSagaCompleted(sagaId: String) = Unit

        override fun markSagaFailed(
            sagaId: String,
            errorMessage: String,
        ) = Unit

        override fun markAwaitingResponse(sagaId: String) = Unit

        override fun findSagaById(sagaId: String): SagaSnapshot? = null
    }

    @Test
    fun `a snapshot of a saga just started has no outcome yet`() {
        val snapshot = SagaSnapshot(id = "saga", type = "Shipping", status = SagaStatus.STARTED, payload = "{}")

        assertNull(snapshot.lastError)
        assertNull(snapshot.completedAt)
        assertNull(snapshot.version)
    }

    @Test
    fun `a step recorded without a payload carries an empty one`() {
        val process = RecordingSagaProcess()

        process.recordSagaStep("saga", Step.SHIP, SagaStepStatus.COMPLETED)

        assertEquals(listOf(emptyMap()), process.payloads.toList())
    }

    /** These names are stored with every saga step, so renaming one strands the rows already written. */
    @Test
    fun `step outcomes keep the names they are stored under`() {
        assertEquals(
            listOf("STARTED", "COMPLETED", "FAILED", "COMPENSATED", "COMPENSATION_FAILED", "PARTIALLY_COMPLETED"),
            SagaStepStatus.entries.map { it.name },
        )
    }
}
