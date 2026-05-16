package com.vertyll.veds.template.application.port.inbound

import com.vertyll.veds.template.domain.model.Template

/**
 * Driving port for the saga-driven template-processing use case.
 *
 * Reference contract — replace with the real use cases when cloning the
 * template service for a new bounded context.
 */
interface TemplateSagaUseCase {
    fun processTemplateWithSaga(
        name: String,
        payload: String,
    ): Template
}
