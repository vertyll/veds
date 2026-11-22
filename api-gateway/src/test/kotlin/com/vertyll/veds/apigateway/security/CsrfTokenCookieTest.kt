package com.vertyll.veds.apigateway.security

import org.junit.jupiter.api.Test
import org.springframework.http.ResponseCookie
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CsrfTokenCookieTest {
    @Test
    fun `the token cookie is Secure, HttpOnly, Strict and bound to this host`() {
        val cookie = savedCookie(secure = true)

        assertEquals("__Host-XSRF-TOKEN", cookie.name)
        assertTrue(cookie.isSecure)
        assertTrue(cookie.isHttpOnly)
        assertEquals("Strict", cookie.sameSite)
        assertEquals("/", cookie.path)
        assertEquals(null, cookie.domain)
        assertTrue(cookie.maxAge.isNegative, "a session cookie, without Max-Age")
    }

    @Test
    fun `without TLS the token cookie drops Secure and the prefix that requires it`() {
        val cookie = savedCookie(secure = false)

        assertEquals("XSRF-TOKEN", cookie.name)
        assertFalse(cookie.isSecure)
        assertTrue(cookie.isHttpOnly)
        assertEquals("Strict", cookie.sameSite)
    }

    @Test
    fun `the token travels back in the X-XSRF-TOKEN header`() {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/session"))

        val token = requireNotNull(CsrfTokenCookie.repository(secure = true).generateToken(exchange).block())

        assertEquals("X-XSRF-TOKEN", token.headerName)
    }

    private fun savedCookie(secure: Boolean): ResponseCookie {
        val repository = CsrfTokenCookie.repository(secure)
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/session"))
        repository.generateToken(exchange).flatMap { repository.saveToken(exchange, it) }.block()
        return exchange.response.cookies.values
            .flatten()
            .single()
    }
}
