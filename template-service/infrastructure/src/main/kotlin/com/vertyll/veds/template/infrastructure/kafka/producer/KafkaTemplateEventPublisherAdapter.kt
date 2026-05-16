package com.vertyll.veds.template.infrastructure.kafka.producer

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.event.Events
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.template.TemplateFailedEvent
import com.vertyll.veds.template.TemplateProcessedEvent
import com.vertyll.veds.template.application.port.outbound.TemplateEventPublisherPort
import com.vertyll.veds.template.infrastructure.kafka.TemplateKafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class KafkaTemplateEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val avroPayloadSerializer: AvroPayloadSerializer,
) : TemplateEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishTemplateProcessed(
        sagaId: String,
        templateId: Long,
        payload: Map<String, Any?>,
    ) {
        val eventId = Events.newId()
        val event =
            TemplateProcessedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setSagaId(sagaId)
                .setTemplateId(templateId)
                .setPayload(payload.mapValues { it.value?.toString() ?: "" })
                .build()
        val bytes = avroPayloadSerializer.serialize(TemplateKafkaTopics.TEMPLATE_PROCESSED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = TemplateKafkaTopics.TEMPLATE_PROCESSED,
            key = eventId,
            payload = bytes,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.info("Published TEMPLATE_PROCESSED for templateId=$templateId (sagaId=$sagaId)")
    }

    override fun publishTemplateFailed(
        sagaId: String,
        templateId: Long?,
        error: String,
    ) {
        val eventId = Events.newId()
        val event =
            TemplateFailedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setSagaId(sagaId)
                .setTemplateId(templateId)
                .setError(error)
                .build()
        val bytes = avroPayloadSerializer.serialize(TemplateKafkaTopics.TEMPLATE_FAILED, event)
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = TemplateKafkaTopics.TEMPLATE_FAILED,
            key = eventId,
            payload = bytes,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.info("Published TEMPLATE_FAILED for templateId=$templateId (sagaId=$sagaId): $error")
    }
}
