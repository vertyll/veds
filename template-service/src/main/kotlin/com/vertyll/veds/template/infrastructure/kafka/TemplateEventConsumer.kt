package com.vertyll.veds.template.infrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.template.TemplateRequestedEvent
import com.vertyll.veds.template.application.service.TemplateSagaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Inbound Kafka adapter (driving adapter) that listens for `template.requested` events
 * and triggers the local template-processing saga.
 */
@Component
class TemplateEventConsumer(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val templateSagaService: TemplateSagaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [TemplateKafkaTopics.TEMPLATE_REQUESTED])
    fun consume(
        record: ConsumerRecord<String, ByteArray>,
        @Payload payload: ByteArray,
    ) {
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
