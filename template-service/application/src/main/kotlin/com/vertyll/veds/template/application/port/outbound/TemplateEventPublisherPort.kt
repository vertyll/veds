package com.vertyll.veds.template.application.port.outbound

/**
 * Outbound port for publishing template-related domain events.
 *
 * Replace method signatures with concrete events when cloning this service for a new microservice.
 * Implementation lives in `infrastructure/kafka/KafkaTemplateEventPublisherAdapter`.
 */
interface TemplateEventPublisherPort {
    fun publishTemplateProcessed(
        sagaId: String,
        templateId: Long,
        payload: Map<String, Any?> = emptyMap(),
    )

    fun publishTemplateFailed(
        sagaId: String,
        templateId: Long?,
        error: String,
    )
}
