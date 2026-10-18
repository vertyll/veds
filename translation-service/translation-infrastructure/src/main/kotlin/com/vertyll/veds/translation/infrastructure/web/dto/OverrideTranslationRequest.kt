package com.vertyll.veds.translation.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank

data class OverrideTranslationRequest(
    @field:NotBlank(message = "validation.translation.value_required")
    val value: String = "",
)
