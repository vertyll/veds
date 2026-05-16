package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.iam.application.service.MailFeedbackService
import com.vertyll.veds.mail.mail.MailFailedEvent
import com.vertyll.veds.mail.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter for mail-delivery feedback topics.
 *
 * Decodes the Avro payload and forwards to [MailFeedbackService]; no business
 * decisions live here.
 */
@Component
internal class MailFeedbackConsumer(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val mailFeedbackService: MailFeedbackService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [IamKafkaTopics.MAIL_SENT])
    fun handleMailSent(
        @Payload payload: ByteArray,
    ) {
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
    ) {
        try {
            val event = avroPayloadDeserializer.deserialize(IamKafkaTopics.MAIL_FAILED, payload) as MailFailedEvent
            mailFeedbackService.handleMailFailed(sagaId = event.sagaId, to = event.to, error = event.error)
        } catch (e: Exception) {
            logger.error("Failed to process MailFailedEvent: {} — will be retried / sent to DLT", e.message, e)
            throw e
        }
    }
}
