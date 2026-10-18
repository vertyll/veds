package com.vertyll.veds.template.application.port.inbound.query

import com.vertyll.veds.template.application.dto.TemplateResponse

@Suppress("kotlin:S6517")
interface TemplateQueryUseCase {
    fun getById(id: String): TemplateResponse
}
