package com.vertyll.veds.sharedinfrastructure.saga.contract

import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus

/**
 * Persistence-agnostic repository port for saga step records.
 *
 * Concrete adapters (JPA, MongoDB, …) implement this interface.
 */
interface SagaStepRepositoryPort<T : SagaStep<T>> {
    fun save(step: T): T

    fun findOneById(id: Long): T?

    fun findBySagaId(sagaId: String): List<T>

    fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<T>

    fun findBySagaIdAndStatus(
        sagaId: String,
        status: SagaStepStatus,
    ): List<T>

    fun findByStepNameAndStatus(
        stepName: String,
        status: SagaStepStatus,
    ): List<T>

    fun findByCompensationStepId(compensationStepId: Long): T?
}
