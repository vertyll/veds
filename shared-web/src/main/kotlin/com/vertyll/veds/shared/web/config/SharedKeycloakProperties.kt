package com.vertyll.veds.shared.web.config

import com.vertyll.veds.shared.web.openapi.SharedOpenApiProperties
import com.vertyll.veds.shared.web.security.KeycloakJwtAuthenticationConverter
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe binding for the `veds.shared.keycloak.*` namespace, whose defaults ship in
 * `shared-web-config.yml` and are loaded by [SharedConfigEnvironmentPostProcessor].
 *
 * Carries the cross-service identity settings consumed by the security and Keycloak-admin
 * layers — notably [KeycloakJwtAuthenticationConverter] and its reactive counterpart.
 *
 * The prefix names Keycloak rather than stopping at `veds.shared`, so that it sits alongside
 * [SharedOpenApiProperties] as a sibling instead of enclosing it. Two classes binding nested
 * prefixes both work, but only until one of them grows a field named after the other.
 */
@ConfigurationProperties(prefix = "veds.shared.keycloak")
data class SharedKeycloakProperties(
    /** Base URL of the Keycloak server. */
    val serverUrl: String,
    /** Realm every service authenticates against. */
    val realm: String,
    /** Client id of the service account used for admin calls. */
    val adminClientId: String,
    /** Secret of that service account. */
    val adminClientSecret: String,
    /** Client id the gateway uses for the authorization-code flow. */
    val gatewayClientId: String,
    /** Secret for that client. */
    val gatewayClientSecret: String,
    /** Dotted path to the roles claim inside the access token. */
    val rolesClaimPath: String,
    /** Attributes of the refresh-token cookie the gateway sets. */
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
