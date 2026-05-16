package com.vertyll.veds.sharedinfrastructure.saga.contract

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus

/**
 * Persistence-agnostic repository port for saga step records.
 *
 * Concrete adapters (JPA, MongoDB, …) implement this interface.
 */
interface SagaStepRepositoryPort<T : SagaStep<T>> {
    /** Persists [step] (insert or update) and returns the stored instance. */
    fun save(step: T): T

    /** Looks up a step by its surrogate id; `null` if absent. */
    fun findOneById(id: Long): T?

    /** Returns every step recorded for the given saga, in insertion order. */
    fun findBySagaId(sagaId: String): List<T>

    /** Returns the steps for [sagaId] whose [SagaStep.stepName] equals [stepName] (typically zero or one). */
    fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<T>

    /** Returns the steps for [sagaId] currently in the given [status]. */
    fun findBySagaIdAndStatus(
        sagaId: String,
        status: SagaStepStatus,
    ): List<T>

    /** Returns steps with the given [stepName] in the given [status] across all sagas. */
    fun findByStepNameAndStatus(
        stepName: String,
        status: SagaStepStatus,
    ): List<T>

    /** Returns the compensation step that reverted the original step with id [compensationStepId]; `null` if not yet compensated. */
    fun findByCompensationStepId(compensationStepId: Long): T?
}
