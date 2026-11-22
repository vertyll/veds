package com.vertyll.veds.apigateway.security

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BearerTokenRequestsTest {
    private val bearerTokenRequests = BearerTokenRequests()

    @Test
    fun `a request with a bearer token matches`() {
        assertTrue(matches(requestWithAuthorization("Bearer eyJhbGciOiJSUzI1NiJ9")))
    }

    @Test
    fun `the scheme matches in any case`() {
        assertTrue(matches(requestWithAuthorization("bearer eyJhbGciOiJSUzI1NiJ9")))
    }

    @Test
    fun `a request with a malformed bearer token matches`() {
        assertTrue(matches(requestWithAuthorization("Bearer not a token")))
    }

    @Test
    fun `a request with another scheme does not match`() {
        assertFalse(matches(requestWithAuthorization("Basic YWRhOnNlY3JldA==")))
    }

    @Test
    fun `a request without an authorization header does not match`() {
        assertFalse(matches(MockServerWebExchange.from(MockServerHttpRequest.post("/tasks"))))
    }

    private fun matches(exchange: MockServerWebExchange): Boolean = requireNotNull(bearerTokenRequests.matches(exchange).block()).isMatch

    private fun requestWithAuthorization(value: String): MockServerWebExchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/tasks").header(HttpHeaders.AUTHORIZATION, value))
}
