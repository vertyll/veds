package com.vertyll.veds.template.infrastructure.kafka.consumer

import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.messaging.kafka.ProcessedEventGuard
import com.vertyll.veds.template.TemplateRequestedEvent
import com.vertyll.veds.template.application.command.CreateTemplateCommand
import com.vertyll.veds.template.application.port.inbound.command.TemplateCommandUseCase
import com.vertyll.veds.template.infrastructure.kafka.TemplateKafkaTopics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class TemplateEventConsumerAdapter(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val templateCommands: TemplateCommandUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "template-service:template-requested"
    }

    @KafkaListener(topics = [TemplateKafkaTopics.TEMPLATE_REQUESTED])
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
            logger.info("Received ${TemplateKafkaTopics.TEMPLATE_REQUESTED} message: key={}", record.key())
            val event = avroPayloadDeserializer.deserialize(TemplateKafkaTopics.TEMPLATE_REQUESTED, payload) as TemplateRequestedEvent
            val name = event.name
            val templatePayload = event.payload.toString()
            templateCommands.processTemplateWithSaga(CreateTemplateCommand(name = name, payload = templatePayload))
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }
}
