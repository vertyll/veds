package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SagaStepJpaRepository :
    JpaRepository<SagaStepJpaEntity, Long>,
    BaseSagaStepRepository<SagaStepJpaEntity> {
    override fun findBySagaId(sagaId: String): List<SagaStepJpaEntity>

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<SagaStepJpaEntity>

    fun findBySagaIdAndStepNameAndStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ): List<SagaStepJpaEntity>

    fun findBySagaIdOrderByCreatedAtDesc(sagaId: String): List<SagaStepJpaEntity>
}
