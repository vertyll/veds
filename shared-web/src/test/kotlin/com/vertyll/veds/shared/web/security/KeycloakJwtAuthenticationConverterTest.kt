package com.vertyll.veds.shared.web.security

import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KeycloakJwtAuthenticationConverterTest {
    private val properties =
        SharedKeycloakProperties(
            serverUrl = "http://localhost:9000",
            realm = "veds",
            adminClientId = "veds-service-account",
            adminClientSecret = "",
            gatewayClientId = "veds-api-gateway",
            gatewayClientSecret = "",
            rolesClaimPath = "realm_access.roles",
            cookie =
                SharedKeycloakProperties.CookieProperties(
                    "KEYCLOAK_REFRESH_TOKEN",
                    httpOnly = true,
                    secure = false,
                    sameSite = "Strict",
                    path = "/",
                ),
        )

    private val converter = KeycloakJwtAuthenticationConverter(properties)

    private fun jwt(claims: Map<String, Any>): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claims { it.putAll(claims) }
            .build()

    @Test
    fun `maps realm roles to Spring authorities`() {
        val token =
            converter.convert(
                jwt(mapOf("sub" to "user-1", "realm_access" to mapOf("roles" to listOf("ADMIN", "USER")))),
            )

        assertEquals(setOf("ROLE_ADMIN", "ROLE_USER"), token.authorities.map { it.authority }.toSet())
        assertEquals("user-1", token.name)
    }

    /**
     * Fail closed. A token whose roles claim cannot be read grants nothing, rather than falling
     * back to a default role.
     */
    @Test
    fun `grants no authority when the roles claim is unreadable`() {
        val token = converter.convert(jwt(mapOf("sub" to "user-1")))

        assertTrue(token.authorities.isEmpty())
    }

    /**
     * The subject is the identity every downstream authorization decision is made against, so an
     * absent one is a broken token rather than an anonymous caller.
     */
    @Test
    fun `refuses a token without a subject`() {
        assertFailsWith<IllegalArgumentException> {
            converter.convert(jwt(mapOf("realm_access" to mapOf("roles" to listOf("ADMIN")))))
        }
    }
}
