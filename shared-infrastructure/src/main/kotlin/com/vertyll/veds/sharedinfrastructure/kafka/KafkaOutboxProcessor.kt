package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Reusable Kafka outbox processor implementing the **transactional outbox**
 * pattern (Richardson, *Microservices Patterns*, ch. 3).
 *
 * Dispatch is a **two-phase** flow that decouples broker IO from the
 * caller's business transaction and prevents the same row from being
 * published twice by concurrent poller instances:
 *  1. **Claim** ([OutboxDispatchTx.claimBatch]) — `SELECT … FOR UPDATE SKIP
 *     LOCKED` + transition rows to `PROCESSING` in a short transaction.
 *  2. **Publish** ([dispatch]) — broker network IO outside any DB
 *     transaction. Status transitions on success/failure happen in fresh
 *     `REQUIRES_NEW` transactions via [OutboxDispatchTx] so the broker call
 *     never holds a DB connection.
 *
 * The transactional helpers live in a separate bean ([OutboxDispatchTx]) so
 * the calls go through Spring's AOP proxy and actually open the requested
 * transaction (self-invocation on `this` would bypass the proxy).
 *
 * Depends only on persistence-agnostic ports ([OutboxRepositoryPort] and
 * [OutboxMessageFactory]) so the outbox can be backed by any storage
 * technology.
 */
@Service
class KafkaOutboxProcessor(
    private val outboxRepository: OutboxRepositoryPort,
    private val outboxMessageFactory: OutboxMessageFactory,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    private val properties: KafkaOutboxProperties,
    private val dispatchTx: OutboxDispatchTx,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val UNKNOWN_ERROR = "Unknown error"
        private const val EVENT_ID_HEADER = "eventId"
    }

    /**
     * Scheduled poll fired every `veds.outbox.poll-interval-ms`
     * (default `5000`). Claims a batch of messages via
     * [OutboxDispatchTx.claimBatch] and dispatches each one to Kafka
     * outside any DB transaction. Safe to run on multiple instances —
     * `FOR UPDATE SKIP LOCKED` guarantees each row is claimed by exactly
     * one poller.
     */
    @Scheduled(fixedDelayString = $$"${veds.outbox.poll-interval-ms:5000}")
    fun pollAndDispatch() {
        val now = Instant.now()
        val claimed =
            dispatchTx.claimBatch(
                maxRetries = properties.maxRetries,
                retriableBefore = now.minus(properties.retryCooldown),
                stuckBefore = now.minus(properties.stuckThreshold),
                batchSize = properties.batchSize,
            )
        if (claimed.isEmpty()) return

        logger.debug("Claimed {} outbox message(s) for dispatch", claimed.size)
        claimed.forEach(::dispatch)
    }

    private fun dispatch(message: OutboxMessage) {
        try {
            val kafkaMessage =
                MessageBuilder
                    .withPayload(message.payload)
                    .setHeader(KafkaHeaders.TOPIC, message.topic)
                    .setHeader(KafkaHeaders.KEY, message.key)
                    .setHeader(EVENT_ID_HEADER, message.eventId)
                    .build()

            val result = kafkaTemplate.send(kafkaMessage).get()

            logger.info(
                "Published outbox message: eventId={}, topic={}, partition={}, offset={}",
                message.eventId,
                message.topic,
                result.recordMetadata.partition(),
                result.recordMetadata.offset(),
            )
            dispatchTx.markCompleted(message)
        } catch (e: Exception) {
            logger.error(
                "Failed to publish outbox message: eventId={}, topic={}, attempt={}/{} — {}",
                message.eventId,
                message.topic,
                message.retryCount + 1,
                properties.maxRetries,
                e.message,
                e,
            )
            dispatchTx.rescheduleOrDeadLetter(
                message = message,
                error = e.message ?: UNKNOWN_ERROR,
                maxRetries = properties.maxRetries,
            )
            if (message.retryCount + 1 >= properties.maxRetries) {
                logger.error(
                    "Dead-lettered outbox message after {} attempts: eventId={}, topic={}",
                    properties.maxRetries,
                    message.eventId,
                    message.topic,
                )
            }
        }
    }

    /**
     * Creates a new outbox message for a raw topic name.
     *
     * The payload must already be serialized to its on-the-wire form
     * ([ByteArray]). Encoding (Avro, JSON, …) is the responsibility of the
     * caller — this processor is transport-agnostic.
     */
    @Transactional
    fun saveOutboxMessage(
        topic: String,
        key: String,
        payload: ByteArray,
        sagaId: String? = null,
        eventId: String? = null,
    ): OutboxMessage {
        val message =
            outboxMessageFactory.create(
                topic = topic,
                key = key,
                payload = payload,
                sagaId = sagaId,
                eventId = eventId,
            )

        return outboxRepository.save(message)
    }
}
