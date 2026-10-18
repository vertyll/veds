package com.vertyll.veds.mail.infrastructure.persistence.repository

import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.saga.engine.persistence.BaseSagaStepRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface SagaStepJpaRepository :
    JpaRepository<SagaStepJpaEntity, Long>,
    BaseSagaStepRepository<SagaStepJpaEntity> {
    override fun findBySagaId(sagaId: String): List<SagaStepJpaEntity>

    override fun findBySagaIdAndStepName(
        sagaId: String,
        stepName: String,
    ): List<SagaStepJpaEntity>
}
