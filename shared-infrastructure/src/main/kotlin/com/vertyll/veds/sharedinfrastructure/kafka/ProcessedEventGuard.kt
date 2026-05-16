package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.ProcessedEventFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.ProcessedEventRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Guard implementing the **idempotent receiver** pattern for Kafka
 * consumers. Wraps the persistence-agnostic
 * [ProcessedEventRepositoryPort] / [ProcessedEventFactory] ports.
 *
 * Typical use inside a `@KafkaListener`:
 * ```
 * fun handle(@Header("eventId") eventId: String?, @Payload payload: ByteArray) {
 *     val id = eventId ?: run { logger.warn("missing eventId — skipping"); return }
 *     if (!processedEventGuard.claim(id, GROUP)) {
 *         logger.info("Duplicate event {} — skipping", id); return
 *     }
 *     // … process …
 * }
 * ```
 *
 * The claim is performed in a `REQUIRES_NEW` transaction so a duplicate is
 * detected even when the surrounding listener method has its own
 * transaction — and so the dedup row is committed even if the business
 * transaction later rolls back; preventing a poison message from being
 * re-processed in an infinite loop.
 */
@Service
class ProcessedEventGuard(
    private val repository: ProcessedEventRepositoryPort,
    private val factory: ProcessedEventFactory,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Atomically claims [eventId] for [consumerGroup].
     *
     * @return `true` if the event was claimed for the first time and the
     * caller should proceed with processing; `false` if it was already
     * processed (duplicate) and the caller should skip.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun claim(
        eventId: String,
        consumerGroup: String,
    ): Boolean =
        try {
            repository.insert(factory.create(eventId = eventId, consumerGroup = consumerGroup))
            true
        } catch (_: DataIntegrityViolationException) {
            logger.debug("Duplicate event detected: eventId={}, consumerGroup={}", eventId, consumerGroup)
            false
        }
}
