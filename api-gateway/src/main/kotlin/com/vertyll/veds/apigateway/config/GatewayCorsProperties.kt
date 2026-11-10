package com.vertyll.veds.apigateway.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Cross-origin policy for the gateway.
 *
 * Bound from `application.gateway.cors.*`. The gateway is a BFF: the browser holds an HttpOnly session
 * cookie, so the preflight response has to allow credentials, and the CORS specification then
 * forbids `*` as an origin. That is why [allowedOrigins] has no wildcard default — a deployment
 * names the front-end it serves.
 *
 * Example:
 * ```yaml
 * application:
 *   gateway:
 *     cors:
 *       allowed-origins:
 *         - https://app.example.com
 * ```
 */
@ConfigurationProperties(prefix = "application.gateway.cors")
@Validated
data class GatewayCorsProperties(
    /** Origins allowed to call the gateway with credentials. */
    @field:NotEmpty
    val allowedOrigins: List<@NotBlank String> = emptyList(),
    @field:NotEmpty
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
    @field:NotEmpty
    val allowedHeaders: List<String> = listOf("*"),
    val allowCredentials: Boolean = true,
)
