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
    val id: Long?
    val sagaId: String
    val stepName: String
    val status: SagaStepStatus
    val payload: String?
    val errorMessage: String?
    val createdAt: Instant
    val completedAt: Instant?
    val compensationStepId: Long?
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
