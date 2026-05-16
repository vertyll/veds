package com.vertyll.veds.mail.infrastructure.persistence.repository

import com.vertyll.veds.mail.infrastructure.persistence.entity.OutboxJpaEntity
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
internal interface OutboxJpaRepository : JpaRepository<OutboxJpaEntity, Long> {
    fun findByStatus(status: OutboxStatus): List<OutboxJpaEntity>

    fun findBySagaId(sagaId: String): List<OutboxJpaEntity>

    fun findByEventId(eventId: String): OutboxJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        SELECT o FROM OutboxJpaEntity o
        WHERE (
            (o.status = com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus.PENDING
                AND o.retryCount < :maxRetries
                AND (o.lastRetryAt IS NULL OR o.lastRetryAt < :retriableBefore))
            OR
            (o.status = com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus.PROCESSING
                AND o.processedAt < :stuckBefore)
        )
        ORDER BY o.createdAt ASC
        """,
    )
    fun lockBatchForDispatch(
        maxRetries: Int,
        retriableBefore: Instant,
        stuckBefore: Instant,
        pageable: Pageable,
    ): List<OutboxJpaEntity>
}
