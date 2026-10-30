package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStepStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The consuming half of compensation: a participating service receives an undo command and has to
 * leave behind an audit trail of what it reversed. The recorded step is what tells an operator
 * afterwards that the undo actually ran.
 */
internal class SagaCompensationEngineTest {
    private data class Command(
        val reason: String,
    )

    private val steps = InMemoryStepRepository()
    private val handled = mutableListOf<Pair<String, Command>>()

    private val stepFactory =
        object : SagaCompensationStepFactory<TestStep> {
            override fun createCompensationStep(draft: CompensationStepDraft) =
                TestStep(
                    sagaId = draft.sagaId,
                    stepName = draft.stepName,
                    status = draft.status,
                    createdAt = draft.createdAt,
                    completedAt = draft.completedAt,
                    compensationStepId = draft.compensationStepId,
                )
        }

    private fun engine(event: DecodedCompensationEvent<Command>) =
        SagaCompensationEngine(
            sagaStepRepository = steps,
            commandDeserializer = { event },
            stepFactory = stepFactory,
            handler = { sagaId, command -> handled += sagaId to command },
        )

    private fun givenOriginalStep(name: String) =
        steps.save(
            TestStep(sagaId = "saga-1", stepName = name, status = SagaStepStatus.COMPLETED),
        )

    @Test
    fun `the undo command reaches the service's own handler`() {
        val command = Command("budget rejected")
        engine(DecodedCompensationEvent("saga-1", null, command)).handleCompensationEvent(byteArrayOf())

        assertEquals(listOf("saga-1" to command), handled)
    }

    @Test
    fun `the reversal is recorded against the step it undid`() {
        val original = givenOriginalStep("ReserveBudget")

        engine(DecodedCompensationEvent("saga-1", original.id, Command("x"))).handleCompensationEvent(byteArrayOf())

        val recorded = steps.stored.single { it.stepName.startsWith(SagaCompensationEngine.COMPENSATION_PREFIX) }
        assertEquals("CompensateReserveBudget", recorded.stepName)
        assertEquals(SagaStepStatus.COMPENSATED, recorded.status)
        assertEquals(original.id, recorded.compensationStepId)
    }

    @Test
    fun `the original step is left untouched`() {
        val original = givenOriginalStep("ReserveBudget")

        engine(DecodedCompensationEvent("saga-1", original.id, Command("x"))).handleCompensationEvent(byteArrayOf())

        assertEquals(SagaStepStatus.COMPLETED, steps.findOneById(original.id!!)!!.status)
    }

    /**
     * A command carrying an id that no longer exists means the two services disagree about the
     * saga's history. Recording the reversal against nothing would hide that, so it fails loudly.
     */
    @Test
    fun `an unknown step id fails instead of silently skipping the audit trail`() {
        val error =
            assertFailsWith<IllegalStateException> {
                engine(DecodedCompensationEvent("saga-1", 404L, Command("x"))).handleCompensationEvent(byteArrayOf())
            }

        assertTrue(error.message!!.contains("404"))
        assertTrue(error.message!!.contains("saga-1"))
    }

    /** Some compensations are not tied to one step; those simply run without leaving a step behind. */
    @Test
    fun `a command without a step id records nothing`() {
        engine(DecodedCompensationEvent("saga-1", null, Command("x"))).handleCompensationEvent(byteArrayOf())

        assertTrue(steps.stored.isEmpty())
        assertEquals(1, handled.size)
    }

    @Test
    fun `the handler runs before the reversal is recorded`() {
        val original = givenOriginalStep("ReserveBudget")
        val engine =
            SagaCompensationEngine(
                sagaStepRepository = steps,
                commandDeserializer = { DecodedCompensationEvent("saga-1", original.id, Command("x")) },
                stepFactory = stepFactory,
                handler = { _, _ -> throw IllegalStateException("undo failed") },
            )

        assertFailsWith<IllegalStateException> { engine.handleCompensationEvent(byteArrayOf()) }

        assertNull(
            steps.stored.firstOrNull { it.stepName.startsWith(SagaCompensationEngine.COMPENSATION_PREFIX) },
            "recorded a reversal that never happened",
        )
    }
}
