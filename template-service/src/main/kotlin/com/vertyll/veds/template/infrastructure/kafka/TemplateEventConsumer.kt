package com.vertyll.veds.template.infrastructure.kafka

import com.vertyll.veds.template.application.service.TemplateSagaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Inbound Kafka adapter (driving adapter) that listens for `template.requested` events
 * and triggers the local template-processing saga.
 *
 * Replace the topic / payload shape with the concrete event contract when cloning this service.
 */
@Component
class TemplateEventConsumer(
    private val objectMapper: ObjectMapper,
    private val templateSagaService: TemplateSagaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [TemplateKafkaTopics.TEMPLATE_REQUESTED])
    fun consume(
        record: ConsumerRecord<String, String>,
        @Payload payload: String,
    ) {
        try {
            logger.info("Received ${TemplateKafkaTopics.TEMPLATE_REQUESTED} message: key={}", record.key())
            val node: JsonNode = objectMapper.readTree(cleanPayload(payload))
            val name = node["name"]?.asString() ?: ""
            val templatePayload = node["payload"]?.asString() ?: node["content"]?.asString() ?: ""
            templateSagaService.processTemplateWithSaga(name, templatePayload)
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }

    private fun cleanPayload(payload: String): String =
        if (payload.startsWith("\"") && payload.endsWith("\"")) {
            payload.substring(1, payload.length - 1).replace("\\\"", "\"")
        } else {
            payload
        }
}
