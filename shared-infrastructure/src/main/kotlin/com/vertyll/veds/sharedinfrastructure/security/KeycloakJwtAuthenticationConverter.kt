package com.vertyll.veds.sharedinfrastructure.security

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import com.vertyll.veds.sharedinfrastructure.utils.KeycloakJwtUtils
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Converts a Keycloak-issued JWT into a Spring Security [JwtAuthenticationToken]
 * with authorities extracted from the configured roles claim.
 *
 * Used by WebMVC-based services (iam-service, mail-service, template-service).
 */
class KeycloakJwtAuthenticationConverter(
    private val sharedConfig: SharedConfigProperties,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = extractRoles(jwt).map { SimpleGrantedAuthority("ROLE_$it") }
        val subject = requireNotNull(jwt.subject) { "JWT is missing the mandatory 'sub' (subject) claim" }
        return JwtAuthenticationToken(jwt, authorities, subject)
    }

    private fun extractRoles(jwt: Jwt) = KeycloakJwtUtils.extractRoles(jwt, sharedConfig.keycloak.rolesClaimPath)
}
