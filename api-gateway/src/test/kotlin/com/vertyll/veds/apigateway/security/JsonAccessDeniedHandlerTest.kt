package com.vertyll.veds.apigateway.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.server.csrf.CsrfException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class JsonAccessDeniedHandlerTest {
    private val handler = JsonAccessDeniedHandler(ObjectMapper())

    @Test
    fun `a write refused for its CSRF token is answered with a code of its own`() {
        val body = refusedBody(CsrfException("Invalid CSRF Token"))

        assertTrue(body.contains("common.csrf_token_invalid"))
        assertFalse(body.contains("common.access_denied"))
    }

    @Test
    fun `any other refusal is answered as access denied`() {
        val body = refusedBody(AccessDeniedException("Denied"))

        assertTrue(body.contains("common.access_denied"))
    }

    private fun refusedBody(denied: AccessDeniedException): String {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/tasks"))

        handler.handle(exchange, denied).block()

        assertEquals(HttpStatus.FORBIDDEN, exchange.response.statusCode)
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, exchange.response.headers.contentType)
        return requireNotNull(exchange.response.bodyAsString.block())
    }
}
