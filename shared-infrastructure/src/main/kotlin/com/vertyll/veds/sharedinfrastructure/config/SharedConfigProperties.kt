package com.vertyll.veds.sharedinfrastructure.config

import com.vertyll.veds.sharedinfrastructure.security.KeycloakJwtAuthenticationConverter
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe binding for the `veds.shared.*` configuration namespace
 * loaded from `shared-config.yml` by [SharedConfigEnvironmentPostProcessor].
 *
 * Carries the cross-service Keycloak/identity settings consumed by the
 * security and Keycloak-admin layers (notably [KeycloakJwtAuthenticationConverter]
 * and its reactive counterpart).
 */
@ConfigurationProperties(prefix = "veds.shared")
data class SharedConfigProperties(
    /** Container for Keycloak server, realm, client and cookie settings. */
    val keycloak: KeycloakProperties,
) {
    /**
     * Keycloak realm / client configuration shared by all services.
     *
     * @property serverUrl Base URL of the Keycloak server (e.g. `http://keycloak:8080`).
     * @property realm Realm name used for both token issuance and admin operations.
     * @property adminClientId Confidential client used by iam-service for the Keycloak Admin REST API.
     * @property adminClientSecret Secret of [adminClientId].
     * @property gatewayClientId Public client used by the API Gateway for the BFF OAuth2 code flow.
     * @property gatewayClientSecret Secret of [gatewayClientId].
     * @property rolesClaimPath Dot-separated path within the JWT to the roles list (e.g. `realm_access.roles`).
     *           Consumed by `KeycloakJwtUtils.extractRoles`.
     * @property cookie Refresh-token cookie configuration used by the gateway BFF.
     */
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
        /**
         * Refresh-token cookie settings used by the API Gateway BFF.
         *
         * @property refreshTokenCookieName Name of the cookie carrying the refresh token.
         * @property httpOnly `Set-Cookie; HttpOnly` flag (block JS access).
         * @property secure `Set-Cookie; Secure` flag (HTTPS-only).
         * @property sameSite `SameSite` policy: `Strict`, `Lax`, or `None`.
         * @property path Path scope of the cookie.
         */
        data class CookieProperties(
            val refreshTokenCookieName: String,
            val httpOnly: Boolean,
            val secure: Boolean,
            val sameSite: String,
            val path: String,
        )
    }
}
