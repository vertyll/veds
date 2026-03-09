package com.vertyll.veds.sharedinfrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "veds.shared")
data class SharedConfigProperties(
    val keycloak: KeycloakProperties,
) {
    data class KeycloakProperties(
        val serverUrl: String,
        val realm: String,
        val adminClientId: String,
        val adminClientSecret: String,
        val gatewayClientId: String,
        val gatewayClientSecret: String,
        val rolesClaimPath: String,
        val cookie: CookieProperties,
    ) {
        data class CookieProperties(
            val refreshTokenCookieName: String,
            val httpOnly: Boolean,
            val secure: Boolean,
            val sameSite: String,
            val path: String,
        )
    }
}
