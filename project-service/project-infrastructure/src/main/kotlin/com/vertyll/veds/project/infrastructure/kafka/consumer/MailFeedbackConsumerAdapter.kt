package com.vertyll.veds.project.infrastructure.kafka.consumer

import com.vertyll.veds.mail.mail.MailFailedEvent
import com.vertyll.veds.mail.mail.MailSentEvent
import com.vertyll.veds.project.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MailFeedbackConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val mailFeedbackService: MailFeedbackUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAIL_SENT = "mail-sent"
        const val MAIL_FAILED = "mail-failed"
        const val CONSUMER_GROUP_MAIL_SENT = "project-service:mail-sent"
        const val CONSUMER_GROUP_MAIL_FAILED = "project-service:mail-failed"
    }

    @KafkaListener(topics = [MAIL_SENT])
    @Transactional
    fun handleMailSent(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (!claim(eventId, CONSUMER_GROUP_MAIL_SENT)) return
        try {
            val event = avroPayloadDeserializer.deserialize(MAIL_SENT, payload) as MailSentEvent
            mailFeedbackService.handleMailSent(sagaId = event.sagaId, to = event.to.toString())
        } catch (e: Exception) {
            logger.error("Failed to process MailSentEvent: {} - will be retried / sent to DLT", e.message, e)
            throw e
        }
    }

    @KafkaListener(topics = [MAIL_FAILED])
    @Transactional
    fun handleMailFailed(
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (!claim(eventId, CONSUMER_GROUP_MAIL_FAILED)) return
        try {
            val event = avroPayloadDeserializer.deserialize(MAIL_FAILED, payload) as MailFailedEvent
            mailFeedbackService.handleMailFailed(
                sagaId = event.sagaId,
                to = event.to,
                error = event.error,
            )
        } catch (e: Exception) {
            logger.error("Failed to process MailFailedEvent: {} - will be retried / sent to DLT", e.message, e)
            throw e
        }
    }

    private fun claim(
        eventId: String?,
        consumerGroup: String,
    ): Boolean {
        if (eventId == null) {
            logger.debug("Event without eventId header on {} - processing without dedupe", consumerGroup)
            return true
        }
        if (!processedEventGuard.claim(eventId, consumerGroup)) {
            logger.info("Skipping duplicate event {} for {}", eventId, consumerGroup)
            return false
        }
        return true
    }
}
