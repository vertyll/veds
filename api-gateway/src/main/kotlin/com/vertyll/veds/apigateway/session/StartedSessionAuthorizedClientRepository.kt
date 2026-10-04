package com.vertyll.veds.apigateway.session

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Keeps authorized clients in the browser's session. Only signing in starts a session: a caller
 * with its own bearer token gets its exchanged tokens anew on every request instead of a
 * session it would never send back.
 */
internal class StartedSessionAuthorizedClientRepository : ServerOAuth2AuthorizedClientRepository {
    private val delegate = WebSessionServerOAuth2AuthorizedClientRepository()

    override fun <T : OAuth2AuthorizedClient> loadAuthorizedClient(
        clientRegistrationId: String,
        principal: Authentication,
        exchange: ServerWebExchange,
    ): Mono<T> =
        exchange.session
            .filter { it.isStarted }
            .flatMap { delegate.loadAuthorizedClient(clientRegistrationId, principal, exchange) }

    override fun saveAuthorizedClient(
        authorizedClient: OAuth2AuthorizedClient,
        principal: Authentication,
        exchange: ServerWebExchange,
    ): Mono<Void> =
        exchange.session
            .filter { it.isStarted || authorizedClient.clientRegistration.registrationId == KeycloakClientConfig.SIGN_IN }
            .flatMap { delegate.saveAuthorizedClient(authorizedClient, principal, exchange) }

    override fun removeAuthorizedClient(
        clientRegistrationId: String,
        principal: Authentication,
        exchange: ServerWebExchange,
    ): Mono<Void> =
        exchange.session
            .filter { it.isStarted }
            .flatMap { delegate.removeAuthorizedClient(clientRegistrationId, principal, exchange) }
}
