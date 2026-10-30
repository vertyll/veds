package com.vertyll.veds.apigateway.security

import com.vertyll.veds.apigateway.config.GatewayOAuthProperties
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.web.server.DefaultServerRedirectStrategy
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Sends the browser back to the front-end after Keycloak: to the page the sign-in was started from, or the front-end's
 * start page, with `?error=` when the sign-in did not complete.
 */
@Component
internal class SignInRedirects(
    private val oauth: GatewayOAuthProperties,
) : ServerAuthenticationSuccessHandler,
    ServerAuthenticationFailureHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    private val redirects = DefaultServerRedirectStrategy()

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication,
    ): Mono<Void> {
        val start = URI.create(oauth.postLoginRedirectUri)
        val page = ReturnPage.onThisSite(webFilterExchange.exchange.getAttribute<String>(ReturnPage.ATTRIBUTE))
        return redirects.sendRedirect(webFilterExchange.exchange, page?.let(start::resolve) ?: start)
    }

    override fun onAuthenticationFailure(
        webFilterExchange: WebFilterExchange,
        exception: AuthenticationException,
    ): Mono<Void> {
        val exchange = webFilterExchange.exchange
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode
        val keycloakError = exchange.request.queryParams.getFirst(OAuth2ParameterNames.ERROR)
        val result =
            when {
                errorCode in STATE_ERRORS -> STATE_MISMATCH
                keycloakError != null -> keycloakError
                else -> CODE_EXCHANGE_FAILED
            }
        log.warn("Sign-in could not be completed: {}", errorCode ?: exception.message)
        val target =
            UriComponentsBuilder
                .fromUriString(oauth.postLoginRedirectUri)
                .queryParam(OAuth2ParameterNames.ERROR, result)
                .encode()
                .build()
                .toUri()
        return redirects.sendRedirect(exchange, target)
    }

    private companion object {
        private const val STATE_MISMATCH = "state_mismatch"
        private const val CODE_EXCHANGE_FAILED = "code_exchange_failed"
        private val STATE_ERRORS = setOf("authorization_request_not_found", "invalid_state_parameter")
    }
}
