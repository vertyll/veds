package com.vertyll.veds.sharedinfrastructure.saga.repository

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStepRepositoryPort
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * JPA flavor of [SagaStepRepositoryPort]. See [BaseSagaRepository] for the
 * port/adapter rationale.
 */
@NoRepositoryBean
interface BaseSagaStepRepository<T : BaseSagaStep<T>> :
    JpaRepository<T, Long>,
    SagaStepRepositoryPort<T> {
    override fun findBySagaId(sagaId: String): List<T>

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<T>

    override fun findBySagaIdAndStatus(
        sagaId: String,
        status: SagaStepStatus,
    ): List<T>

    override fun findByStepNameAndStatus(
        stepName: String,
        status: SagaStepStatus,
    ): List<T>

    override fun findByCompensationStepId(compensationStepId: Long): T?

    // Bridges between the port (T?) and JpaRepository (Optional<T>).
    override fun findOneById(id: Long): T? = findById(id).orElse(null)
}
