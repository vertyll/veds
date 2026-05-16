package com.vertyll.veds.template.application.dto

import jakarta.validation.constraints.NotBlank

data class CreateTemplateRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
)
