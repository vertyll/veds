package com.vertyll.veds.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Cross-origin policy for the gateway.
 *
 * Bound from `application.gateway.cors.*`. The gateway is a BFF: the browser holds an HttpOnly session
 * cookie, so the preflight response has to allow credentials, and the CORS specification then
 * forbids `*` as an origin. That is why [allowedOrigins] has no wildcard default — a deployment
 * names the front end it serves.
 *
 * Example:
 * ```yaml
 * veds:
 *   gateway:
 *     cors:
 *       allowed-origins:
 *         - https://app.example.com
 * ```
 */
@ConfigurationProperties(prefix = "application.gateway.cors")
data class GatewayCorsProperties(
    /** Origins allowed to call the gateway with credentials. */
    val allowedOrigins: List<String> = emptyList(),
    /** HTTP methods allowed on a cross-origin request. */
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
    /** Request headers the browser may send. */
    val allowedHeaders: List<String> = listOf("*"),
    /** Whether the browser may send the session cookie. */
    val allowCredentials: Boolean = true,
)
