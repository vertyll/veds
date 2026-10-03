package com.vertyll.veds.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Browser leg of the login flow, bound from `application.gateway.oauth.*`. */
@ConfigurationProperties(prefix = "application.gateway.oauth")
data class GatewayOAuthProperties(
    /** Callback Keycloak redirects the browser to with the authorization code. */
    val redirectUri: String,
    /** Front-end page the browser lands on once the session is established. */
    val postLoginRedirectUri: String,
)
