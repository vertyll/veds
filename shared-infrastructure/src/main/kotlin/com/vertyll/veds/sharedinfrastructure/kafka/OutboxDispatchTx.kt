package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Transactional helpers for [KafkaOutboxProcessor].
 *
 * Extracted into a separate bean so that calls through the Spring proxy
 * actually open the requested transaction — `@Transactional` methods that
 * are self-invoked on the same instance bypass the proxy and silently run
 * without a transaction.
 */
@Component
class OutboxDispatchTx(
    private val outboxRepository: OutboxRepositoryPort,
) {
    /**
     * Short transaction: select + lock a batch with `FOR UPDATE SKIP LOCKED`
     * and transition each row to `PROCESSING`. Commits immediately, freeing
     * the lock so other pollers can proceed.
     */
    @Transactional
    fun claimBatch(
        maxRetries: Int,
        retriableBefore: Instant,
        stuckBefore: Instant,
        batchSize: Int,
    ): List<OutboxMessage> =
        outboxRepository
            .lockBatchForDispatch(
                maxRetries = maxRetries,
                retriableBefore = retriableBefore,
                stuckBefore = stuckBefore,
                batchSize = batchSize,
            ).map { outboxRepository.save(it.markProcessing()) }

    /**
     * Fresh `REQUIRES_NEW` transaction that marks [message] as
     * successfully published. Called after the Kafka broker has
     * acknowledged to send.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markCompleted(message: OutboxMessage) {
        outboxRepository.save(message.markCompleted())
    }

    /**
     * Fresh `REQUIRES_NEW` transaction that either:
     *  - schedules [message] for a retry (status `RETRY_SCHEDULED`,
     *    `retryCount` incremented), or
     *  - dead-letters it if it has exhausted [maxRetries].
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun rescheduleOrDeadLetter(
        message: OutboxMessage,
        error: String,
        maxRetries: Int,
    ) {
        if (message.retryCount + 1 >= maxRetries) {
            outboxRepository.save(message.markDeadLettered(error))
        } else {
            outboxRepository.save(message.markRetryScheduled(error))
        }
    }
}
