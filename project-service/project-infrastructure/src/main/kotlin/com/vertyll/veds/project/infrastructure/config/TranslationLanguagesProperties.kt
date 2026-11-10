package com.vertyll.veds.project.infrastructure.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Languages this service accepts on translatable fields.
 *
 * Bound from `application.translation.*`. The list is configured here rather than read from
 * translation-service; [SupportedLanguagesAdapter] is the only file that reads it.
 *
 * Example:
 * ```yaml
 * application:
 *   translation:
 *     supported-languages: pl,en
 * ```
 */
@ConfigurationProperties(prefix = "application.translation")
@Validated
data class TranslationLanguagesProperties(
    /** Language tags accepted on translatable fields. Must not be empty. */
    @field:NotEmpty
    val supportedLanguages: List<@NotBlank String> = emptyList(),
)
