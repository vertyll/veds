package com.vertyll.veds.template.application.port.outbound

interface TemplateEventPublisherPort {
    fun publishTemplateProcessed(
        sagaId: String,
        templateId: Long,
        payload: Map<String, Any> = emptyMap(),
    )

    fun publishTemplateFailed(
        sagaId: String,
        templateId: Long?,
        error: String,
    )
}
