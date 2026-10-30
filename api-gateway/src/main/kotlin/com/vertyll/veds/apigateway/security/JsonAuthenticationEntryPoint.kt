package com.vertyll.veds.apigateway.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.veds.shared.web.error.Problems
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
internal class JsonAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : ServerAuthenticationEntryPoint {
    override fun commence(
        exchange: ServerWebExchange,
        e: AuthenticationException,
    ): Mono<Void> =
        Mono.defer {
            val response = exchange.response
            if (response.isCommitted) return@defer Mono.empty()

            response.statusCode = HttpStatus.UNAUTHORIZED
            response.headers.contentType = MediaType.APPLICATION_PROBLEM_JSON

            val problem =
                Problems.of(
                    status = HttpStatus.UNAUTHORIZED,
                    code = NOT_AUTHENTICATED,
                    instance = exchange.request.path.value(),
                )

            val buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(problem))
            response.writeWith(Mono.just(buffer))
        }

    private companion object {
        private const val NOT_AUTHENTICATED = "common.not_authenticated"
    }
}
