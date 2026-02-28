package com.vertyll.veds.sharedinfrastructure.kafka

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface KafkaOutboxRepository : JpaRepository<KafkaOutbox, Long> {
    fun findByStatus(status: KafkaOutbox.OutboxStatus): List<KafkaOutbox>

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
        status: KafkaOutbox.OutboxStatus,
        maxRetries: Int,
        minRetryTime: Instant,
    ): List<KafkaOutbox>

    @Modifying
    @Query("UPDATE KafkaOutbox k SET k.status = :newStatus, k.processedAt = :now WHERE k.id = :id")
    fun updateStatus(
        id: Long,
        newStatus: KafkaOutbox.OutboxStatus,
        now: Instant,
    )

    @Modifying
    @Query(
        "UPDATE KafkaOutbox k SET k.status = :newStatus, k.errorMessage = :errorMessage, k.retryCount = k.retryCount + 1 WHERE k.id = :id",
    )
    fun markAsFailed(
        id: Long,
        newStatus: KafkaOutbox.OutboxStatus,
        errorMessage: String,
    )
}
