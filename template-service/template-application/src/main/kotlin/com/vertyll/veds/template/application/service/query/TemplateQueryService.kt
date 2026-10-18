package com.vertyll.veds.template.application.service.query

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.template.application.dto.TemplateResponse
import com.vertyll.veds.template.application.port.inbound.query.TemplateQueryUseCase
import com.vertyll.veds.template.domain.error.TemplateError
import com.vertyll.veds.template.domain.repository.TemplateRepository

class TemplateQueryService(
    private val templateRepository: TemplateRepository,
) : TemplateQueryUseCase {
    override fun getById(id: String): TemplateResponse {
        val template =
            templateRepository.findById(id)
                ?: throw ApiException(TemplateError.NOT_FOUND, mapOf("id" to id))
        return TemplateResponse.from(template)
    }
}
