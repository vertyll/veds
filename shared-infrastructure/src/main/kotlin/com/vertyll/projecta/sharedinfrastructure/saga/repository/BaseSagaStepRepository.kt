package com.vertyll.projecta.sharedinfrastructure.saga.repository

import com.vertyll.projecta.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface BaseSagaStepRepository<T : BaseSagaStep> : JpaRepository<T, Long> {
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
