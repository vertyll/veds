package com.vertyll.veds.mail.infrastructure.kafka.consumer

import com.vertyll.veds.mail.application.port.inbound.EmailSagaUseCase
import com.vertyll.veds.mail.infrastructure.kafka.MailKafkaTopics
import com.vertyll.veds.mail.mail.MailRequestedCommand
import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MailCommandConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val emailSagaService: EmailSagaUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "mail-service:mail-requested"
    }

    @KafkaListener(topics = [MailKafkaTopics.MAIL_REQUESTED])
    @Transactional
    fun consume(
        record: ConsumerRecord<String, ByteArray>,
        @Payload payload: ByteArray,
        @Header(name = "eventId", required = false) eventId: String?,
    ) {
        if (eventId != null && !processedEventGuard.claim(eventId, CONSUMER_GROUP)) {
            logger.info("Skipping duplicate event {} on {}", eventId, record.topic())
            return
        }
        try {
            logger.info("Received {} message: key={}", MailKafkaTopics.MAIL_REQUESTED, record.key())
            val event = avroPayloadDeserializer.deserialize(MailKafkaTopics.MAIL_REQUESTED, payload) as MailRequestedCommand
            emailSagaService.sendEmailWithSaga(
                to = event.to,
                templateName = event.templateName,
                variables = event.variables,
                replyTo = event.replyTo,
                originSagaId = event.sagaId,
                originalEventId = event.eventId,
            )
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }
}
