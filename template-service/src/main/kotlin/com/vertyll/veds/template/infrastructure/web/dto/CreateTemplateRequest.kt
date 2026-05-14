package com.vertyll.veds.template.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank

data class CreateTemplateRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
)
