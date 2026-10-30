package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStepStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Compensation is the only way a distributed workflow can be undone — there is no rollback to fall
 * back on. These tests pin the ordering, the terminal-state guards and what happens when undoing a
 * step itself fails, because getting any of them wrong leaves the system half-committed.
 */
internal class SagaCompensationRunnerTest {
    private data class Command(
        val step: String,
    )

    private val sagas = InMemorySagaRepository()
    private val steps = InMemoryStepRepository()

    // Records what compensation was asked to undo, and in which order.
    private val compensated = mutableListOf<String>()
    private var failOn: String? = null

    private val compensator =
        object : SagaCompensator<TestSaga, TestStep, Command> {
            override fun compensateStep(
                saga: TestSaga,
                step: TestStep,
                context: SagaCompensationContext<Command>,
            ) {
                if (step.stepName == failOn) error("remote service refused")
                compensated += step.stepName
                context.publishCompensationEvent(saga.id, step.id, Command(step.stepName))
            }
        }

    private val published = mutableListOf<Pair<String, Long?>>()

    private val context =
        object : SagaCompensationContext<Command> {
            override fun publishCompensationEvent(
                sagaId: String,
                stepId: Long?,
                command: Command,
            ) {
                published += sagaId to stepId
            }

            override fun readStepPayload(payload: String?): Map<String, Any?> = emptyMap()
        }

    private val runner = SagaCompensationRunner(sagas, steps, compensator, context)

    private fun givenSaga(status: SagaStatus = SagaStatus.COMPENSATING) =
        sagas.save(TestSaga(id = "saga-1", type = "CreateProject", status = status, payload = "{}"))

    private fun givenStep(
        name: String,
        status: SagaStepStatus = SagaStepStatus.COMPLETED,
        createdAt: Instant,
    ) = steps.save(TestStep(sagaId = "saga-1", stepName = name, status = status, createdAt = createdAt))

    @Test
    fun `an unknown saga is an error, not a silent no-op`() {
        val error = assertFailsWith<IllegalArgumentException> { runner.runCompensation("does-not-exist") }

        assertTrue(error.message!!.contains("does-not-exist"))
    }

    /**
     * The watchdog and the failure path can both ask for the same saga, so a second request has to
     * be harmless rather than undo already-settled work twice.
     */
    @Test
    fun `a saga that already reached a terminal state is left alone`() {
        listOf(SagaStatus.COMPLETED, SagaStatus.COMPENSATED, SagaStatus.FAILED).forEach { terminal ->
            sagas.stored.clear()
            val saga = givenSaga(terminal)
            givenStep("ReserveBudget", createdAt = Instant.now())

            runner.runCompensation(saga.id)

            assertEquals(terminal, sagas.findOneById(saga.id)!!.status, "status changed for $terminal")
            assertTrue(compensated.isEmpty(), "compensated a step of a $terminal saga")
        }
    }

    @Test
    fun `a saga that never completed a step is compensated immediately`() {
        val saga = givenSaga()

        runner.runCompensation(saga.id)

        assertEquals(SagaStatus.COMPENSATED, sagas.findOneById(saga.id)!!.status)
    }

    /** Undo runs newest-first: a later step may depend on what an earlier one produced. */
    @Test
    fun `completed steps are compensated in reverse order`() {
        val saga = givenSaga()
        val start = Instant.now()
        givenStep("ReserveBudget", createdAt = start)
        givenStep("AssignOwner", createdAt = start.plusSeconds(1))
        givenStep("NotifyTeam", createdAt = start.plusSeconds(2))

        runner.runCompensation(saga.id)

        assertEquals(listOf("NotifyTeam", "AssignOwner", "ReserveBudget"), compensated)
    }

    @Test
    fun `every compensated step and the saga itself are marked compensated`() {
        val saga = givenSaga()
        givenStep("ReserveBudget", createdAt = Instant.now())

        runner.runCompensation(saga.id)

        assertEquals(SagaStatus.COMPENSATED, sagas.findOneById(saga.id)!!.status)
        assertTrue(steps.findBySagaId(saga.id).all { it.status == SagaStepStatus.COMPENSATED })
        assertEquals(listOf<Pair<String, Long?>>(saga.id to 1L), published)
    }

    /**
     * A step that cannot be undone must not stop the others — the remaining ones are still
     * compensated, and only the saga's own status records that the undo is incomplete.
     */
    @Test
    fun `a failing step does not abort compensation of the rest`() {
        val saga = givenSaga()
        val start = Instant.now()
        givenStep("ReserveBudget", createdAt = start)
        givenStep("AssignOwner", createdAt = start.plusSeconds(1))
        failOn = "AssignOwner"

        runner.runCompensation(saga.id)

        assertEquals(listOf("ReserveBudget"), compensated)
        assertEquals(SagaStatus.COMPENSATION_FAILED, sagas.findOneById(saga.id)!!.status)
    }

    @Test
    fun `a step that could not be undone records the reason`() {
        val saga = givenSaga()
        givenStep("AssignOwner", createdAt = Instant.now())
        failOn = "AssignOwner"

        runner.runCompensation(saga.id)

        val step = steps.findBySagaId(saga.id).single()
        assertEquals(SagaStepStatus.COMPENSATION_FAILED, step.status)
        assertEquals("remote service refused", step.errorMessage)
    }

    /** What makes the watchdog's retry meaningful: a previously failed undo is picked up again. */
    @Test
    fun `a previously failed compensation is retried on the next run`() {
        val saga = givenSaga()
        givenStep("AssignOwner", status = SagaStepStatus.COMPENSATION_FAILED, createdAt = Instant.now())

        runner.runCompensation(saga.id)

        assertEquals(listOf("AssignOwner"), compensated)
        assertEquals(SagaStatus.COMPENSATED, sagas.findOneById(saga.id)!!.status)
    }

    /** Steps still in flight, or already undone, are not touched a second time. */
    @Test
    fun `in-flight and already compensated steps are skipped`() {
        val saga = givenSaga()
        val start = Instant.now()
        givenStep("InFlight", status = SagaStepStatus.STARTED, createdAt = start)
        givenStep("AlreadyUndone", status = SagaStepStatus.COMPENSATED, createdAt = start.plusSeconds(1))

        runner.runCompensation(saga.id)

        assertTrue(compensated.isEmpty())
        assertEquals(SagaStatus.COMPENSATED, sagas.findOneById(saga.id)!!.status)
    }
}
