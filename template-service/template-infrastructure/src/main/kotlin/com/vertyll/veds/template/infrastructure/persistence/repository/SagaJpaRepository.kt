package com.vertyll.veds.template.infrastructure.persistence.repository

import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.engine.persistence.BaseSagaRepository
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
internal interface SagaJpaRepository :
    JpaRepository<SagaJpaEntity, String>,
    BaseSagaRepository<SagaJpaEntity> {
    override fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<SagaJpaEntity>
}
