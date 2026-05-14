package com.vertyll.veds.template.infrastructure.web.controller

import com.vertyll.veds.template.application.service.TemplateSagaService
import com.vertyll.veds.template.infrastructure.response.ApiResponse
import com.vertyll.veds.template.infrastructure.web.dto.CreateTemplateRequest
import com.vertyll.veds.template.infrastructure.web.dto.TemplateResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Reference HTTP adapter — replace with your real endpoints when cloning this service.
 */
@RestController
@RequestMapping("/template")
class TemplateController(
    private val templateSagaService: TemplateSagaService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody
        request: CreateTemplateRequest,
    ): ResponseEntity<ApiResponse<TemplateResponse>> {
        val template = templateSagaService.processTemplateWithSaga(request.name, request.payload)
        return ApiResponse.buildResponse(
            data = TemplateResponse.from(template),
            message = "Template processed successfully",
            status = HttpStatus.CREATED,
        )
    }
}
