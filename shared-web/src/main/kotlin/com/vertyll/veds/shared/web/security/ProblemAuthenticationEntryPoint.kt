package com.vertyll.veds.shared.web.security

import com.vertyll.veds.shared.web.error.Problems
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.AuthenticationEntryPoint
import tools.jackson.databind.ObjectMapper

/**
 * Answers an unauthenticated request with the same RFC 9457 document as every other
 * refusal.
 *
 * Spring's [BearerTokenAuthenticationEntryPoint] sets `WWW-Authenticate` and leaves
 * the body empty, which RFC 6750 allows but this platform does not: a caller that
 * reads `code` to decide what to tell the reader would get nothing here and have to
 * special-case one status. The challenge header still goes out — it is what makes
 * the response a valid `401` — and the body is added beside it.
 *
 * Serialized with the container's [ObjectMapper] on purpose: a plain one nests the
 * extension members under `properties`.
 */
class ProblemAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    private companion object {
        private const val NOT_AUTHENTICATED = "common.not_authenticated"
    }

    private val challenge = BearerTokenAuthenticationEntryPoint()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        challenge.commence(request, response, authException)
        if (response.isCommitted) return

        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.outputStream,
            Problems.of(HttpStatus.UNAUTHORIZED, NOT_AUTHENTICATED, request.requestURI),
        )
    }
}
