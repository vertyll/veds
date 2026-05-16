package com.vertyll.veds.sharedinfrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessage
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxMessageFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxRepositoryPort
import com.vertyll.veds.sharedinfrastructure.kafka.contract.OutboxStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Reusable Kafka outbox processor.
 *
 * Depends only on persistence-agnostic ports ([OutboxRepositoryPort] for
 * storage and [OutboxMessageFactory] for instantiating new messages) so the
 * outbox can be backed by any storage technology (JPA today, MongoDB or
 * others by providing alternative port implementations).
 */
@Service
class KafkaOutboxProcessor(
    private val outboxRepository: OutboxRepositoryPort,
    private val outboxMessageFactory: OutboxMessageFactory,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val MAX_RETRIES = 3
        private const val UNKNOWN_ERROR = "Unknown error"
        private const val RETRY_DELAY_MINUTES = 5L
        private const val SECONDS_PER_MINUTE = 60L
    }

    /**
     * Scheduled job that processes pending messages from the outbox.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    fun processOutboxMessages() {
        val minRetryTime = Instant.now().minusSeconds(RETRY_DELAY_MINUTES * SECONDS_PER_MINUTE)
        val pendingMessages =
            outboxRepository.findMessagesToProcess(
                OutboxStatus.PENDING,
                MAX_RETRIES,
                minRetryTime,
            )

        logger.info("Found ${pendingMessages.size} pending messages to process")

        pendingMessages.forEach { message ->
            try {
                val processing = outboxRepository.save(message.markProcessing())

                val kafkaMessage =
                    MessageBuilder
                        .withPayload(processing.payload)
                        .setHeader(KafkaHeaders.TOPIC, processing.topic)
                        .setHeader(KafkaHeaders.KEY, processing.key)
                        .setHeader("eventId", processing.eventId)
                        .build()

                val result = kafkaTemplate.send(kafkaMessage).get()

                logger.info(
                    "Successfully sent message to Kafka: topic=${processing.topic}, " +
                        "partition=${result.recordMetadata.partition()}, " +
                        "offset=${result.recordMetadata.offset()}",
                )

                outboxRepository.save(processing.markCompleted())
            } catch (e: Exception) {
                logger.error("Failed to process outbox message id=${message.id}: ${e.message}", e)
                outboxRepository.save(message.markFailed(e.message ?: UNKNOWN_ERROR))
            }
        }
    }

    /**
     * Creates a new outbox message for a raw topic name.
     */
    @Transactional
    fun saveOutboxMessage(
        topic: String,
        key: String,
        payload: Any,
        sagaId: String? = null,
        eventId: String? = null,
    ): OutboxMessage {
        val payloadBytes =
            when (payload) {
                is ByteArray -> payload
                is String -> payload.toByteArray(Charsets.UTF_8)
                else -> objectMapper.writeValueAsBytes(payload)
            }

        val message =
            outboxMessageFactory.create(
                topic = topic,
                key = key,
                payload = payloadBytes,
                sagaId = sagaId,
                eventId = eventId,
            )

        return outboxRepository.save(message)
    }
}
