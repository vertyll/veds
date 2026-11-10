package com.vertyll.veds.shared.web.config

import com.vertyll.veds.shared.web.openapi.SharedOpenApiProperties
import com.vertyll.veds.shared.web.security.KeycloakJwtAuthenticationConverter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Type-safe binding for the `application.shared.keycloak.*` namespace, whose defaults ship in
 * `shared-web-config.yml` and are loaded by [SharedConfigEnvironmentPostProcessor].
 *
 * Carries the cross-service identity settings consumed by the security and Keycloak-admin
 * layers — notably [KeycloakJwtAuthenticationConverter] and its reactive counterpart.
 *
 * The prefix names Keycloak rather than stopping at `application.shared`, so that it sits alongside
 * [SharedOpenApiProperties] as a sibling instead of enclosing it. Two classes binding nested
 * prefixes both work, but only until one of them grows a field named after the other.
 */
@ConfigurationProperties(prefix = "application.shared.keycloak")
@Validated
data class SharedKeycloakProperties(
    /** Base URL of the Keycloak server. */
    @field:NotBlank
    val serverUrl: String,
    /** Realm every service authenticates against. */
    @field:NotBlank
    val realm: String,
    /** Client id of the service account used for admin calls. */
    @field:NotBlank
    val adminClientId: String,
    /** Secret of that service account. */
    @field:NotBlank
    val adminClientSecret: String,
    /** Client id the gateway uses for the authorization-code flow. */
    @field:NotBlank
    val gatewayClientId: String,
    /** Secret for that client. */
    @field:NotBlank
    val gatewayClientSecret: String,
    /** Dotted path to the roles claim inside the access token. */
    @field:NotBlank
    val rolesClaimPath: String,
    /** Attributes of the cookies the gateway sets. */
    @field:Valid
    val cookie: CookieProperties,
) {
    /** The attributes the gateway's cookies share. */
    data class CookieProperties(
        /** Whether the cookies travel over HTTPS only. */
        val secure: Boolean,
        /** The `Path` attribute of the cookies. */
        @field:NotBlank
        val path: String,
    )
}
