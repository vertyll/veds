package com.vertyll.veds.apigateway.security

import com.vertyll.veds.apigateway.session.KeycloakClientConfig
import com.vertyll.veds.shared.web.security.ReactiveKeycloakJwtAuthenticationConverter
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Turns a signed-in session into the person's access token for the rest of the request: the
 * token is refreshed when it is about to expire, verified like a bearer token and becomes the
 * request's authentication, so routes and `/auth/session` see a JWT whether the caller has a
 * session or a token of its own. A refresh Keycloak refuses ends the session.
 */
internal class SessionAccessTokenFilter(
    private val authorizedClients: ReactiveOAuth2AuthorizedClientManager,
    private val accessTokens: ReactiveJwtDecoder,
    private val tokenAuthentication: ReactiveKeycloakJwtAuthenticationConverter,
) : WebFilter {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> =
        ReactiveSecurityContextHolder
            .getContext()
            .mapNotNull<OAuth2AuthenticationToken> { it.authentication as? OAuth2AuthenticationToken }
            .flatMap { session -> accessAuthentication(session, exchange) }
            .defaultIfEmpty(Replacement.Keep)
            .flatMap { replacement ->
                when (replacement) {
                    Replacement.Keep -> {
                        chain.filter(exchange)
                    }

                    Replacement.Anonymous -> {
                        chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.clearContext())
                    }

                    is Replacement.Token -> {
                        chain
                            .filter(exchange.mutate().principal(Mono.just(replacement.authentication)).build())
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(replacement.authentication))
                    }
                }
            }

    private fun accessAuthentication(
        session: OAuth2AuthenticationToken,
        exchange: ServerWebExchange,
    ): Mono<Replacement> =
        authorizedClients
            .authorize(
                OAuth2AuthorizeRequest
                    .withClientRegistrationId(KeycloakClientConfig.SIGN_IN)
                    .principal(session)
                    .attribute(ServerWebExchange::class.java.name, exchange)
                    .build(),
            ).flatMap { client -> accessTokens.decode(client.accessToken.tokenValue) }
            .flatMap { jwt -> tokenAuthentication.convert(jwt) }
            .map<Replacement> { Replacement.Token(it) }
            .defaultIfEmpty(Replacement.Anonymous)
            .onErrorResume(ClientAuthorizationException::class.java) { e ->
                if (e.error.errorCode == OAuth2ErrorCodes.INVALID_GRANT) {
                    log.info("Session of {} ended: Keycloak refused the refresh", session.name)
                    exchange.session.flatMap { it.invalidate() }.thenReturn(Replacement.Anonymous)
                } else {
                    log.warn("Could not refresh the session of {}: {}", session.name, e.error.errorCode)
                    Mono.just(Replacement.Anonymous)
                }
            }.onErrorResume(JwtException::class.java) { e ->
                log.warn("Access token of {} rejected: {}", session.name, e.message)
                Mono.just(Replacement.Anonymous)
            }

    private sealed interface Replacement {
        data object Keep : Replacement

        data object Anonymous : Replacement

        data class Token(
            val authentication: Authentication,
        ) : Replacement
    }
}
