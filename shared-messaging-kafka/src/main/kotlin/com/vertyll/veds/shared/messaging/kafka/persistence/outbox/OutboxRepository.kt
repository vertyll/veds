package com.vertyll.veds.shared.messaging.kafka.persistence.outbox

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Spring Data access to the outbox table. A service enables it by naming this
 * package in `@EnableJpaRepositories`.
 */
@Repository
interface OutboxRepository : JpaRepository<OutboxEntity, Long> {
    /** The messages in [status]. */
    fun findByStatus(status: OutboxStatus): List<OutboxEntity>

    /** The messages published for the saga [sagaId]. */
    fun findBySagaId(sagaId: String): List<OutboxEntity>

    /** The message with [eventId], if there is one. */
    fun findByEventId(eventId: String): OutboxEntity?

    /**
     * Claims the next batch to dispatch under a skip-locked pessimistic write
     * lock, so several instances of the same service poll the table without
     * publishing the same row twice.
     *
     * The batch holds rows still awaiting their first or a further attempt, and
     * rows left in `PROCESSING` by an instance that died mid-dispatch — those
     * would otherwise sit there forever.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        SELECT o FROM OutboxEntity o
        WHERE (
            (o.status = com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus.PENDING
                AND o.retryCount < :maxRetries
                AND (o.lastRetryAt IS NULL OR o.lastRetryAt < :retriableBefore))
            OR
            (o.status = com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus.PROCESSING
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
    ): List<OutboxEntity>
}
