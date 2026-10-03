package com.vertyll.veds.project.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Languages this service accepts on translatable fields.
 *
 * Bound from `application.translation.*`. Seeded here until translation-service owns the catalogue;
 * [SupportedLanguagesAdapter] is the only file that changes when it does.
 *
 * Example:
 * ```yaml
 * veds:
 *   translation:
 *     supported-languages: pl,en
 * ```
 */
@ConfigurationProperties(prefix = "application.translation")
data class TranslationLanguagesProperties(
    /** Language tags accepted on translatable fields. Must not be empty. */
    val supportedLanguages: List<String> = emptyList(),
)
