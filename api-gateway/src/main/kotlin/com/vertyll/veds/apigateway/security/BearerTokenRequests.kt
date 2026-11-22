package com.vertyll.veds.apigateway.security

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Requests authenticated by a bearer token, which need no CSRF token: no browser adds an `Authorization` header on its
 * own, and another site cannot set one without a CORS preflight the gateway refuses. Any other scheme still rides on the
 * session cookie. A malformed bearer token matches too, as the resource server refuses it with `401`.
 */
internal class BearerTokenRequests : ServerWebExchangeMatcher {
    private val bearerTokens = ServerBearerTokenAuthenticationConverter()

    override fun matches(exchange: ServerWebExchange): Mono<MatchResult> =
        bearerTokens
            .convert(exchange)
            .flatMap { MatchResult.match() }
            .switchIfEmpty(MatchResult.notMatch())
            .onErrorResume(OAuth2AuthenticationException::class.java) { MatchResult.match() }
}
