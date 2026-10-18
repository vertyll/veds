package com.vertyll.veds.translation.infrastructure.web.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class RegisterCatalogueRequest(
    @field:NotBlank(message = "validation.translation.source_service_required")
    val sourceService: String = "",
    @field:NotEmpty(message = "validation.translation.entries_required")
    val entries: List<@Valid CatalogueEntryRequest> = emptyList(),
)
