package com.vertyll.veds.mail.infrastructure.kafka

import com.vertyll.veds.iam.mail.MailRequestedEvent
import com.vertyll.veds.mail.application.service.EmailSagaService
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter for the `mail-requested` topic.
 *
 * Single responsibility: decode the Avro payload and delegate the use case to
 * the application layer ([EmailSagaService]). No business decisions here.
 */
@Component
internal class MailEventConsumer(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val emailSagaService: EmailSagaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [MailKafkaTopics.MAIL_REQUESTED])
    fun consume(
        record: ConsumerRecord<String, ByteArray>,
        @Payload payload: ByteArray,
    ) {
        try {
            logger.info("Received {} message: key={}", MailKafkaTopics.MAIL_REQUESTED, record.key())
            val event = avroPayloadDeserializer.deserialize(MailKafkaTopics.MAIL_REQUESTED, payload) as MailRequestedEvent
            emailSagaService.sendEmailWithSaga(
                to = event.to,
                subject = event.subject,
                templateName = event.templateName,
                variables = event.variables ?: emptyMap(),
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
