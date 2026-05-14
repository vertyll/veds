package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SagaJpaRepository :
    JpaRepository<SagaJpaEntity, String>,
    BaseSagaRepository<SagaJpaEntity> {
    override fun findByTypeAndStatus(
        type: String,
        status: SagaStatus,
    ): List<SagaJpaEntity>

    fun findByStatusAndUpdatedAtBefore(
        status: SagaStatus,
        updatedAt: Instant,
    ): List<SagaJpaEntity>
}
