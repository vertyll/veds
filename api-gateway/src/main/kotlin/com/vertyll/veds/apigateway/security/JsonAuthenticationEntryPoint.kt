package com.vertyll.veds.apigateway.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.vertyll.veds.apigateway.infrastructure.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JsonAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : ServerAuthenticationEntryPoint {
    override fun commence(
        exchange: ServerWebExchange,
        e: AuthenticationException,
    ): Mono<Void> =
        Mono.defer {
            val response = exchange.response
            response.statusCode = HttpStatus.UNAUTHORIZED
            response.headers.contentType = MediaType.APPLICATION_JSON

            val apiResponse =
                ApiResponse.of(
                    data = null,
                    message = "Unauthorized: ${e.message}",
                )

            val bytes = objectMapper.writeValueAsBytes(apiResponse)
            val buffer = response.bufferFactory().wrap(bytes)

            response.writeWith(Mono.just(buffer))
        }
}
