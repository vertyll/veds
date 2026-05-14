package com.vertyll.veds.template.infrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.template.application.port.out.TemplateEventPublisherPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Outbound Kafka adapter for template domain events.
 *
 * Uses the transactional outbox pattern via [KafkaOutboxProcessor] to guarantee at-least-once delivery
 * without dual-write issues. Replace topic names / event payload shape when cloning this service.
 */
@Component
internal class KafkaTemplateEventPublisherAdapter(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
) : TemplateEventPublisherPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publishTemplateProcessed(
        sagaId: String,
        templateId: Long,
        payload: Map<String, Any?>,
    ) {
        val eventId = UUID.randomUUID().toString()
        val event =
            mapOf(
                "eventId" to eventId,
                "eventType" to TemplateEventTypes.TEMPLATE_PROCESSED,
                "timestamp" to Instant.now().toString(),
                "sagaId" to sagaId,
                "templateId" to templateId,
                "payload" to payload,
            )
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = TemplateKafkaTopics.TEMPLATE_PROCESSED,
            key = eventId,
            payload = event,
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
        val eventId = UUID.randomUUID().toString()
        val event =
            mapOf(
                "eventId" to eventId,
                "eventType" to TemplateEventTypes.TEMPLATE_FAILED,
                "timestamp" to Instant.now().toString(),
                "sagaId" to sagaId,
                "templateId" to templateId,
                "error" to error,
            )
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = TemplateKafkaTopics.TEMPLATE_FAILED,
            key = eventId,
            payload = event,
            sagaId = sagaId,
            eventId = eventId,
        )
        logger.info("Published TEMPLATE_FAILED for templateId=$templateId (sagaId=$sagaId): $error")
    }
}

internal object TemplateKafkaTopics {
    const val TEMPLATE_REQUESTED = "template.requested"
    const val TEMPLATE_PROCESSED = "template.processed"
    const val TEMPLATE_FAILED = "template.failed"
}

internal object TemplateEventTypes {
    const val TEMPLATE_REQUESTED = "TEMPLATE_REQUESTED"
    const val TEMPLATE_PROCESSED = "TEMPLATE_PROCESSED"
    const val TEMPLATE_FAILED = "TEMPLATE_FAILED"
}
