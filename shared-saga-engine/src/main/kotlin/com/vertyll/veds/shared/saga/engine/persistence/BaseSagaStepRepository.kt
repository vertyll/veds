package com.vertyll.veds.shared.saga.engine.persistence

import com.vertyll.veds.shared.saga.SagaStepStatus
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

    override fun findOneById(id: Long): T? = findById(id).orElse(null)
}
