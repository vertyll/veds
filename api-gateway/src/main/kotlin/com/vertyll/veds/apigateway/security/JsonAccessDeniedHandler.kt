package com.vertyll.veds.apigateway.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.veds.shared.web.error.Problems
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.security.web.server.csrf.CsrfException
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Answers a request the gateway refuses with a `403` problem document. A write refused for its CSRF token gets a code of
 * its own, so the caller can tell a token that is missing or stale, which a fresh one fixes, from a refusal no retry
 * changes.
 */
@Component
internal class JsonAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : ServerAccessDeniedHandler {
    override fun handle(
        exchange: ServerWebExchange,
        denied: AccessDeniedException,
    ): Mono<Void> =
        Mono.defer {
            val response = exchange.response
            if (response.isCommitted) return@defer Mono.empty()

            response.statusCode = HttpStatus.FORBIDDEN
            response.headers.contentType = MediaType.APPLICATION_PROBLEM_JSON

            val problem =
                Problems.of(
                    status = HttpStatus.FORBIDDEN,
                    code = if (denied is CsrfException) CSRF_TOKEN_INVALID else ACCESS_DENIED,
                    instance = exchange.request.path.value(),
                )

            val buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(problem))
            response.writeWith(Mono.just(buffer))
        }

    private companion object {
        private const val ACCESS_DENIED = "common.access_denied"
        private const val CSRF_TOKEN_INVALID = "common.csrf_token_invalid"
    }
}
