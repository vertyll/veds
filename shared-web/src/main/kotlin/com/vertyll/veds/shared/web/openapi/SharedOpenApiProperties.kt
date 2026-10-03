package com.vertyll.veds.shared.web.openapi

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Externalized configuration for the generated OpenAPI document.
 *
 * Bound from `application.shared.openapi.*`. Every value has a default, so a service that says
 * nothing still publishes a document named after itself — [title] falls back to
 * `spring.application.name`.
 *
 * Example:
 * ```yaml
 * veds:
 *   shared:
 *     openapi:
 *       title: iam-service
 *       version: 1.4.0
 * ```
 */
@ConfigurationProperties(prefix = "application.shared.openapi")
data class SharedOpenApiProperties(
    /** Document title. Falls back to `spring.application.name` when unset. */
    val title: String? = null,
    /** Version reported in the document. */
    val version: String = DEFAULT_VERSION,
    /** Overrides the generated description when a service wants its own wording. */
    val description: String? = null,
) {
    companion object {
        const val DEFAULT_VERSION: String = "0.0.1-SNAPSHOT"
    }
}
