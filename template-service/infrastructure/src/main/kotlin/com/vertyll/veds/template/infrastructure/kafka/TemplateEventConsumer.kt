package com.vertyll.veds.template.infrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.kafka.ProcessedEventGuard
import com.vertyll.veds.template.TemplateRequestedEvent
import com.vertyll.veds.template.application.port.inbound.TemplateSagaUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter for `template.requested`. Dedupes via
 * [ProcessedEventGuard] (idempotent receiver pattern).
 */
@Component
internal class TemplateEventConsumer(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val templateSagaService: TemplateSagaUseCase,
    private val processedEventGuard: ProcessedEventGuard,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val CONSUMER_GROUP = "template-service:template-requested"
    }

    @KafkaListener(topics = [TemplateKafkaTopics.TEMPLATE_REQUESTED])
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
            val templatePayload = event.payload ?: event.content ?: ""
            templateSagaService.processTemplateWithSaga(name, templatePayload)
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }
}
