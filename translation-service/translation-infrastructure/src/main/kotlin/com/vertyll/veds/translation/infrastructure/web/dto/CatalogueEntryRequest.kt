package com.vertyll.veds.translation.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class CatalogueEntryRequest(
    @field:NotBlank(message = "validation.translation.key_required")
    val key: String = "",
    val description: String? = null,
    @field:NotEmpty(message = "validation.translation.default_values_required")
    val defaultValues: Map<String, String> = emptyMap(),
)
