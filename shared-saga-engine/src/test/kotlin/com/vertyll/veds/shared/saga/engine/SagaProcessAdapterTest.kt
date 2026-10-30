package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.SagaTypeValue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** What an application service sees of a saga: every question and transition goes through this port. */
class SagaProcessAdapterTest {
    private enum class Names(
        override val value: String,
    ) : SagaTypeValue {
        INVITATION("Invitation"),
        SEND_INVITE("SendInvite"),
    }

    private val process =
        SagaProcessAdapter(
            SagaEngine(
                sagaRepository = InMemorySagaRepository(),
                sagaStepRepository = InMemoryStepRepository(),
                objectMapper = ObjectMapper(),
                entityFactory = TestEntityFactory(),
                compensationRunner = {},
            ),
        )

    private fun startedSaga() = process.startSaga(Names.INVITATION, mapOf("email" to "a@example.com"))

    private fun statusOf(sagaId: String) = assertNotNull(process.findSagaById(sagaId)).status

    @Test
    fun `a started saga is handed back as a snapshot of what was stored`() {
        val snapshot = startedSaga()

        assertEquals("Invitation", snapshot.type)
        assertEquals(SagaStatus.STARTED, snapshot.status)
        assertContains(snapshot.payload, "a@example.com")
        assertNull(snapshot.completedAt)
    }

    @Test
    fun `a saga settled as completed reads as completed`() {
        val saga = startedSaga()
        process.recordSagaStep(saga.id, Names.SEND_INVITE, SagaStepStatus.COMPLETED)

        process.markSagaCompleted(saga.id)

        assertEquals(SagaStatus.COMPLETED, statusOf(saga.id))
    }

    @Test
    fun `a failed saga keeps the reason it failed`() {
        val saga = startedSaga()

        process.markSagaFailed(saga.id, "the address bounced")

        assertEquals("the address bounced", process.findSagaById(saga.id)?.lastError)
    }

    @Test
    fun `a saga waiting on another service says so`() {
        val saga = startedSaga()

        process.markAwaitingResponse(saga.id)

        assertEquals(SagaStatus.AWAITING_RESPONSE, statusOf(saga.id))
    }

    @Test
    fun `an unknown saga finds nothing`() {
        assertNull(process.findSagaById("no-such-saga"))
    }
}
