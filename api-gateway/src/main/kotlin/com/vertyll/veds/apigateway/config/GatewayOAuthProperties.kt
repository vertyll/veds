package com.vertyll.veds.apigateway.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/** Browser leg of the login flow and the services behind the gateway, bound from `application.gateway.oauth.*`. */
@ConfigurationProperties(prefix = "application.gateway.oauth")
@Validated
data class GatewayOAuthProperties(
    /** Callback Keycloak redirects the browser to with the authorization code. */
    @field:NotBlank
    val redirectUri: String,
    /** Front-end page the browser lands on once the session is established. */
    @field:NotBlank
    val postLoginRedirectUri: String,
    /** Keycloak clients of the services; a route relays a token exchanged for exactly one of them. */
    @field:NotEmpty
    val serviceAudiences: List<String>,
)
