package com.vertyll.veds.sharedinfrastructure.saga.contract

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import java.time.Instant

/**
 * Persistence-agnostic contract for a single step of a saga.
 *
 * Implementations can be backed by any storage technology (JPA, MongoDB,
 * DynamoDB, in-memory, …). The engine and adapters only depend on this
 * interface, never on a concrete entity class.
 *
 * All fields are exposed as read-only (`val`). State transitions are
 * performed exclusively through behavior methods that encapsulate the
 * aggregate's invariants. See [Saga] for the rationale behind this design.
 */
interface SagaStep<T : SagaStep<T>> {
    /** Storage-assigned surrogate id; `null` until the row is first persisted. */
    val id: Long?

    /** Owning saga's id ([Saga.id]). */
    val sagaId: String

    /** Step name; conventionally a [SagaTypeValue.value]. Compensation rows are prefixed with `Compensate`. */
    val stepName: String

    /** Current lifecycle [SagaStepStatus]; see the enum's KDoc for terminal/transient semantics. */
    val status: SagaStepStatus

    /** Step-local payload as a JSON string; `null` when the step carries no data. */
    val payload: String?

    /** Latest failure reason if the step failed or its compensation failed; `null` otherwise. */
    val errorMessage: String?

    /** Instant the step row was created. */
    val createdAt: Instant

    /** Instant the step reached a terminal status; `null` while it is still in progress. */
    val completedAt: Instant?

    /** When this step compensates another, the id of the originally-completed step it reverts. */
    val compensationStepId: Long?

    /** JPA optimistic-locking version, or `null` for storage backends that do not provide one. */
    val version: Long?

    /** Transitions to [SagaStepStatus.COMPLETED] and stamps `completedAt`. */
    fun markCompleted(): T

    /**
     * Transitions to [SagaStepStatus.FAILED] and stores [error] as
     * `errorMessage`.
     */
    fun markFailed(error: String): T

    /** Transitions to [SagaStepStatus.COMPENSATED]. */
    fun markCompensated(): T

    /**
     * Transitions to [SagaStepStatus.COMPENSATION_FAILED] and stores [error]
     * as `errorMessage`.
     */
    fun markCompensationFailed(error: String?): T

    /** Records the id of the compensating step that reverted this one. */
    fun linkToCompensationStep(compensationStepId: Long): T
}
