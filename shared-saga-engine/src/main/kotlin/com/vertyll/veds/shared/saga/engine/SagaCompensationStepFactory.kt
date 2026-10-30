package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.SagaStepStatus
import java.time.Instant

/**
 * Factory hook used by [SagaCompensationEngine] to create a service-specific
 * compensation step instance against the persistence-agnostic [SagaStep]
 * contract.
 */
@Suppress("kotlin:S6517")
interface SagaCompensationStepFactory<T : SagaStep<T>> {
    /**
     * Builds a fresh compensation [SagaStep] for the owning service. The
     * returned instance must reference the original step it compensates via
     * [CompensationStepDraft.compensationStepId] so the engine can correlate them.
     */
    fun createCompensationStep(draft: CompensationStepDraft): T
}

/** What a compensation step records, before a service gives it its own persistent form. */
data class CompensationStepDraft(
    /** The saga the step belongs to. */
    val sagaId: String,
    /** The name of the compensation step. */
    val stepName: String,
    /** The status the step is recorded with. */
    val status: SagaStepStatus,
    /** When the step was created. */
    val createdAt: Instant,
    /** When the compensation finished. */
    val completedAt: Instant,
    /** The id of the step this one compensates. */
    val compensationStepId: Long,
)
