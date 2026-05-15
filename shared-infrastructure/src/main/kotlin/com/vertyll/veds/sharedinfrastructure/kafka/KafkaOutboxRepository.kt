package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Spring Data JPA repository for [KafkaOutbox]. This is a JPA-flavored
 * persistence adapter that is wrapped by [KafkaOutboxJpaAdapter] to satisfy
 * the persistence-agnostic [com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort].
 *
 * Adding a new persistence flavor (e.g. MongoDB) means providing a parallel
 * adapter implementing the port — this interface stays JPA-only.
 */
@Repository
interface KafkaOutboxRepository : JpaRepository<KafkaOutbox, Long> {
    fun findByStatus(status: OutboxStatus): List<KafkaOutbox>

    fun findBySagaId(sagaId: String): List<KafkaOutbox>

    @Query(
        """
        SELECT k FROM KafkaOutbox k
        WHERE k.status = :status
        AND k.retryCount < :maxRetries
        AND (k.lastRetryAt IS NULL OR k.lastRetryAt < :minRetryTime)
        """,
    )
    fun findMessagesToProcess(
        status: OutboxStatus,
        maxRetries: Int,
        minRetryTime: Instant,
    ): List<KafkaOutbox>

    @Modifying
    @Query("UPDATE KafkaOutbox k SET k.status = :newStatus, k.processedAt = :now WHERE k.id = :id")
    fun updateStatus(
        id: Long,
        newStatus: OutboxStatus,
        now: Instant,
    )

    @Modifying
    @Query(
        "UPDATE KafkaOutbox k SET k.status = :newStatus, k.errorMessage = :errorMessage, k.retryCount = k.retryCount + 1 WHERE k.id = :id",
    )
    fun markAsFailed(
        id: Long,
        newStatus: OutboxStatus,
        errorMessage: String,
    )
}
