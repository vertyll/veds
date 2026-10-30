package com.vertyll.veds.shared.web.openapi

import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedOpenApiAutoConfigurationTest {
    private val configuration = SharedOpenApiAutoConfiguration()

    private fun environment(applicationName: String? = null) =
        MockEnvironment().apply { if (applicationName != null) setProperty("spring.application.name", applicationName) }

    /**
     * Left to spring doc's defaults every service publishes a document called `OpenAPI definition`,
     * so no two of them can be told apart in a browser tab or a generated client.
     */
    @Test
    fun `names the document after the service when nothing is configured`() {
        val api = configuration.sharedOpenApi(SharedOpenApiProperties(), environment("iam-service"))

        assertEquals("iam-service", api.info.title)
        assertTrue(api.info.description.contains("iam-service"))
    }

    @Test
    fun `prefers an explicitly configured title and version`() {
        val api =
            configuration.sharedOpenApi(
                SharedOpenApiProperties(title = "Identity API", version = "2.1.0", description = "Custom"),
                environment("iam-service"),
            )

        assertEquals("Identity API", api.info.title)
        assertEquals("2.1.0", api.info.version)
        assertEquals("Custom", api.info.description)
    }

    /**
     * Almost every endpoint requires a JWT. Without the scheme Swagger UI shows no Authorize
     * button, which makes "Try it out" useless against a running service.
     */
    @Test
    fun `declares a bearer JWT scheme and applies it to the document`() {
        val api = configuration.sharedOpenApi(SharedOpenApiProperties(), environment("file-service"))

        val scheme = api.components.securitySchemes["bearer-jwt"]
        assertNotNull(scheme)
        assertEquals("bearer", scheme.scheme)
        assertEquals("JWT", scheme.bearerFormat)
        assertTrue(api.security.single().containsKey("bearer-jwt"))
    }

    @Test
    fun `falls back to a generic name when the application is unnamed`() {
        val api = configuration.sharedOpenApi(SharedOpenApiProperties(), environment())

        assertEquals("veds-service", api.info.title)
    }
}
