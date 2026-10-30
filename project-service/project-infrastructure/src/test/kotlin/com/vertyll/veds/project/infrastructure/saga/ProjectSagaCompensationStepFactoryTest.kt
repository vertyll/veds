package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.CompensationStepDraft
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

internal class ProjectSagaCompensationStepFactoryTest {
    @Test
    fun `a compensation step keeps what the draft says, and points at the step it undoes`() {
        val startedAt = Instant.parse("2026-01-01T10:00:00Z")
        val endedAt = Instant.parse("2026-01-01T10:00:05Z")

        val step =
            ProjectSagaCompensationStepFactory().createCompensationStep(
                CompensationStepDraft(
                    sagaId = "saga-1",
                    stepName = "ReleaseInvitation",
                    status = SagaStepStatus.COMPENSATED,
                    createdAt = startedAt,
                    completedAt = endedAt,
                    compensationStepId = 7,
                ),
            )

        assertEquals("saga-1", step.sagaId)
        assertEquals("ReleaseInvitation", step.stepName)
        assertEquals(SagaStepStatus.COMPENSATED, step.status)
        assertEquals(startedAt, step.createdAt)
        assertEquals(endedAt, step.completedAt)
        assertEquals(7, step.compensationStepId)
    }
}
