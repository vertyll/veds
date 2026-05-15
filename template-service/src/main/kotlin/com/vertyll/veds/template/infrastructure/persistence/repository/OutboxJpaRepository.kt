package com.vertyll.veds.template.infrastructure.persistence.repository

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import com.vertyll.veds.template.infrastructure.persistence.entity.OutboxJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
internal interface OutboxJpaRepository : JpaRepository<OutboxJpaEntity, Long> {
    fun findByStatus(status: OutboxStatus): List<OutboxJpaEntity>

    fun findBySagaId(sagaId: String): List<OutboxJpaEntity>

    @Query(
        """
        SELECT k FROM OutboxJpaEntity k
        WHERE k.status = :status
        AND k.retryCount < :maxRetries
        AND (k.lastRetryAt IS NULL OR k.lastRetryAt < :minRetryTime)
        """,
    )
    fun findMessagesToProcess(
        status: OutboxStatus,
        maxRetries: Int,
        minRetryTime: Instant,
    ): List<OutboxJpaEntity>
}
