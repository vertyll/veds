package com.vertyll.veds.shared.saga.engine.persistence

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Every service maps its saga table onto these two superclasses, so the transitions here are the
 * one definition of what a saga's row may become. The watchdog selects on `updatedAt` and reporting
 * reads `completedAt`, which is why each transition is checked for the bookkeeping it leaves
 * behind, not only for the status it sets.
 */
class SagaStateMachineTest {
    private class TestSaga(
        status: SagaStatus = SagaStatus.STARTED,
    ) : BaseSaga<TestSaga>(
            id = "saga-1",
            type = "CreateProject",
            status = status,
            payload = "{}",
            startedAt = Instant.now(),
            updatedAt = Instant.EPOCH,
        ) {
        override fun self() = this
    }

    private class TestStep(
        status: SagaStepStatus = SagaStepStatus.STARTED,
    ) : BaseSagaStep<TestStep>(
            sagaId = "saga-1",
            stepName = "ReserveBudget",
            status = status,
            createdAt = Instant.now(),
        ) {
        override fun self() = this
    }

    @Test
    fun `completing a saga closes it and stamps when`() {
        val saga = TestSaga().markCompleted()

        assertEquals(SagaStatus.COMPLETED, saga.status)
        assertNotNull(saga.completedAt)
    }

    /** Waiting is not finishing: the saga stays open so the watchdog can still time it out. */
    @Test
    fun `awaiting a response leaves the saga unfinished`() {
        val saga = TestSaga().markAwaitingResponse()

        assertEquals(SagaStatus.AWAITING_RESPONSE, saga.status)
        assertNull(saga.completedAt)
    }

    @Test
    fun `failing a saga records the reason`() {
        val saga = TestSaga().markFailed("participant rejected")

        assertEquals(SagaStatus.FAILED, saga.status)
        assertEquals("participant rejected", saga.lastError)
        assertNotNull(saga.completedAt)
    }

    /**
     * A saga that starts compensating is not finished — the undo still has to run, and closing it
     * here would hide an unfinished rollback.
     */
    @Test
    fun `starting compensation records the cause and keeps the saga open`() {
        val saga = TestSaga().startCompensating("timeout")

        assertEquals(SagaStatus.COMPENSATING, saga.status)
        assertEquals("timeout", saga.lastError)
        assertNull(saga.completedAt)
    }

    @Test
    fun `both compensation outcomes close the saga`() {
        assertEquals(SagaStatus.COMPENSATED, TestSaga().markCompensated().status)
        assertEquals(SagaStatus.COMPENSATION_FAILED, TestSaga().markCompensationFailed().status)
        assertNotNull(TestSaga().markCompensated().completedAt)
        assertNotNull(TestSaga().markCompensationFailed().completedAt)
    }

    /** The watchdog picks work by age alone, so a transition that forgot this would be invisible. */
    @Test
    fun `every transition refreshes the saga's age`() {
        val transitions =
            listOf<(TestSaga) -> TestSaga>(
                { it.markCompleted() },
                { it.markAwaitingResponse() },
                { it.markFailed("e") },
                { it.startCompensating("e") },
                { it.markCompensated() },
                { it.markCompensationFailed() },
            )

        transitions.forEach { transition ->
            val saga = TestSaga()

            transition(saga)

            assertTrue(saga.updatedAt > Instant.EPOCH, "a transition left updatedAt untouched")
        }
    }

    @Test
    fun `completing a step stamps when it finished`() {
        val step = TestStep().markCompleted()

        assertEquals(SagaStepStatus.COMPLETED, step.status)
        assertNotNull(step.completedAt)
    }

    @Test
    fun `a failed step keeps the error for the compensator to read`() {
        val step = TestStep().markFailed("insufficient funds")

        assertEquals(SagaStepStatus.FAILED, step.status)
        assertEquals("insufficient funds", step.errorMessage)
    }

    /**
     * Compensating a step that previously failed must not erase why it failed — that message is
     * what an operator has to work from.
     */
    @Test
    fun `compensating a failed step preserves the original error`() {
        val step = TestStep().markFailed("insufficient funds").markCompensated()

        assertEquals(SagaStepStatus.COMPENSATED, step.status)
        assertEquals("insufficient funds", step.errorMessage)
    }

    @Test
    fun `a compensation that itself fails records its own reason`() {
        val step = TestStep().markCompensationFailed("service unreachable")

        assertEquals(SagaStepStatus.COMPENSATION_FAILED, step.status)
        assertEquals("service unreachable", step.errorMessage)
    }

    /** A failure with no message must not blank out an error the step already carried. */
    @Test
    fun `a compensation failure without a message keeps what was already known`() {
        val step = TestStep().markFailed("insufficient funds").markCompensationFailed(null)

        assertEquals(SagaStepStatus.COMPENSATION_FAILED, step.status)
        assertEquals("insufficient funds", step.errorMessage)
    }

    @Test
    fun `a step can be linked to the step that undid it`() {
        assertEquals(42L, TestStep().linkToCompensationStep(42L).compensationStepId)
    }
}
