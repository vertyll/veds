package com.vertyll.veds.sharedinfrastructure.security

import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import com.vertyll.veds.sharedinfrastructure.util.KeycloakJwtUtils
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono

/**
 * Reactive variant of [KeycloakJwtAuthenticationConverter] for the API Gateway (WebFlux).
 * Converts a Keycloak-issued JWT into a Spring Security [JwtAuthenticationToken]
 * with authorities extracted from the configured roles claim.
 */
class ReactiveKeycloakJwtAuthenticationConverter(
    private val sharedConfig: SharedConfigProperties,
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {
    override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
        val authorities = extractRoles(jwt).map { SimpleGrantedAuthority("ROLE_$it") }
        return Mono.just(JwtAuthenticationToken(jwt, authorities, jwt.subject))
    }

    private fun extractRoles(jwt: Jwt) = KeycloakJwtUtils.extractRoles(jwt, sharedConfig.keycloak.rolesClaimPath)
}
