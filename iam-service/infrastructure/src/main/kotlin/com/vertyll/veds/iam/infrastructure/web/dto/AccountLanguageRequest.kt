package com.vertyll.veds.iam.infrastructure.web.dto

import jakarta.validation.constraints.Pattern

/**
 * A BCP 47 language tag, such as `pl` or `en-GB`. The languages themselves are the translation service's data, so the
 * tag is checked for its shape only; Keycloak shows its pages in its default language for one it does not have.
 */
data class AccountLanguageRequest(
    @field:Pattern(regexp = "[a-z]{2,3}(-[A-Za-z0-9]{2,8})*", message = "validation.iam.language_invalid")
    val language: String,
)
