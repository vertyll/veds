package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.Pattern

data class AccountLanguageRequest(
    @field:Pattern(regexp = "pl|en", message = "validation.iam.language_unsupported")
    val language: String,
)
