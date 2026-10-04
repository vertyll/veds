package com.vertyll.veds.apigateway.controller

import com.vertyll.veds.apigateway.session.KeycloakClientConfig
import com.vertyll.veds.apigateway.session.KeycloakSessions
import com.vertyll.veds.shared.web.security.ScopedToCaller
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/auth")
internal class AuthController(
    private val authorizedClients: ServerOAuth2AuthorizedClientRepository,
    private val keycloakSessions: KeycloakSessions,
) {
    private companion object {
        private const val REALM_ACCESS_CLAIM = "realm_access"
        private const val ROLES_CLAIM = "roles"
        private const val DEFAULT_ROLES_PREFIX = "default-roles-"
        private val KEYCLOAK_OWN_ROLES = setOf("offline_access", "uma_authorization")
        private val NOBODY =
            AnonymousAuthenticationToken("logout", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
    }

    data class SessionResponse(
        val userId: String,
        val email: String,
        val roles: List<String>,
    )

    @Suppress("kotlin:S6508")
    @GetMapping("/session")
    @ScopedToCaller("reads the session cookie the caller already holds, and answers 204 when there is none")
    fun session(exchange: ServerWebExchange): Mono<ResponseEntity<SessionResponse>> =
        exchange
            .getPrincipal<Authentication>()
            .ofType(JwtAuthenticationToken::class.java)
            .map { authentication ->
                val token = authentication.token
                ResponseEntity.ok(
                    SessionResponse(
                        userId = authentication.name,
                        email = token.getClaimAsString("email").orEmpty(),
                        roles = applicationRoles(token.getClaimAsMap(REALM_ACCESS_CLAIM)?.get(ROLES_CLAIM)),
                    ),
                )
            }.defaultIfEmpty(ResponseEntity.noContent().build())

    @Suppress("kotlin:S6508")
    @PostMapping("/logout")
    @ScopedToCaller("invalidates the refresh token in the caller's own session")
    fun logout(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> =
        exchange
            .getPrincipal<Authentication>()
            .defaultIfEmpty(NOBODY)
            .flatMap { principal ->
                authorizedClients.loadAuthorizedClient<OAuth2AuthorizedClient>(KeycloakClientConfig.SIGN_IN, principal, exchange)
            }.mapNotNull<String> { it.refreshToken?.tokenValue }
            .flatMap(keycloakSessions::revoke)
            .then(exchange.session.flatMap { it.invalidate() })
            .thenReturn(ResponseEntity.noContent().build())

    private fun applicationRoles(claim: Any?): List<String> =
        (claim as? Collection<*>)
            .orEmpty()
            .filterIsInstance<String>()
            .filterNot { it in KEYCLOAK_OWN_ROLES || it.startsWith(DEFAULT_ROLES_PREFIX) }
}
