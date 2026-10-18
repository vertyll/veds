package com.vertyll.veds.project.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

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
data class TranslationLanguagesProperties(
    /** Language tags accepted on translatable fields. Must not be empty. */
    val supportedLanguages: List<String> = emptyList(),
)
