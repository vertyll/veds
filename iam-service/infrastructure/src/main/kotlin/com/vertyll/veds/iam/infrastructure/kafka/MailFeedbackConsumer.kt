package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.iam.application.service.MailFeedbackService
import com.vertyll.veds.mail.mail.MailFailedEvent
import com.vertyll.veds.mail.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter for mail-delivery feedback topics.
 *
 * Implements the **idempotent receiver** pattern via [ProcessedEventGuard]:
 * each event is claimed exactly once per consumer group, so at-least-once
 * delivery from the outbox cannot translate into duplicate business
 * processing.
 */
@Component
class MailFeedbackConsumer(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val mailFeedbackService: MailFeedbackService,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP_MAIL_SENT = "iam-service:mail-sent"
        const val CONSUMER_GROUP_MAIL_FAILED = "iam-service:mail-failed"
    }

    @KafkaListener(topics = [IamKafkaTopics.MAIL_SENT])
    fun handleMailSent(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (!claim(eventId, CONSUMER_GROUP_MAIL_SENT)) return
        try {
            val event = avroPayloadDeserializer.deserialize(IamKafkaTopics.MAIL_SENT, payload) as MailSentEvent
            mailFeedbackService.handleMailSent(sagaId = event.sagaId, to = event.to)
        } catch (e: Exception) {
            logger.error("Failed to process MailSentEvent: {} — will be retried / sent to DLT", e.message, e)
            throw e
        }
    }

    @KafkaListener(topics = [IamKafkaTopics.MAIL_FAILED])
    fun handleMailFailed(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (!claim(eventId, CONSUMER_GROUP_MAIL_FAILED)) return
        try {
            val event = avroPayloadDeserializer.deserialize(IamKafkaTopics.MAIL_FAILED, payload) as MailFailedEvent
            mailFeedbackService.handleMailFailed(sagaId = event.sagaId, to = event.to, error = event.error)
        } catch (e: Exception) {
            logger.error("Failed to process MailFailedEvent: {} — will be retried / sent to DLT", e.message, e)
            throw e
        }
    }

    private fun claim(
        eventId: String?,
        consumerGroup: String,
    ): Boolean {
        if (eventId == null) {
            logger.warn("Missing eventId header on {} — skipping dedup, processing once-best-effort", consumerGroup)
            return true
        }
        val claimed = processedEventGuard.claim(eventId, consumerGroup)
        if (!claimed) {
            logger.info("Skipping duplicate event: eventId={}, consumerGroup={}", eventId, consumerGroup)
        }
        return claimed
    }
}
