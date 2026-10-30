package com.vertyll.veds.shared.web.security

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProblemAuthenticationEntryPointTest {
    @Test
    fun `a caller without a token gets a bearer challenge and a problem document`() {
        val response = MockHttpServletResponse()

        ProblemAuthenticationEntryPoint(JsonMapper.builder().build()).commence(
            MockHttpServletRequest("GET", "/tasks"),
            response,
            BadCredentialsException("no token"),
        )

        assertEquals(401, response.status)
        assertEquals("application/problem+json", response.contentType?.substringBefore(';'))
        assertTrue(response.getHeader("WWW-Authenticate").orEmpty().startsWith("Bearer"))
        assertContains(response.contentAsString, "common.not_authenticated")
    }
}
