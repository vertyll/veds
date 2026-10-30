package com.vertyll.veds.shared.web.security

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.assertEquals

class KeycloakJwtUtilsTest {
    private fun jwt(claims: Map<String, Any>): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claims { it.putAll(claims) }
            .build()

    @Test
    fun `walks Keycloak's nested realm_access layout`() {
        val roles =
            KeycloakJwtUtils.extractRoles(
                jwt(mapOf("realm_access" to mapOf("roles" to listOf("ADMIN", "USER")))),
                "realm_access.roles",
            )

        assertEquals(listOf("ADMIN", "USER"), roles)
    }

    @Test
    fun `reads a flat claim when the path has one segment`() {
        assertEquals(listOf("USER"), KeycloakJwtUtils.extractRoles(jwt(mapOf("roles" to listOf("USER"))), "roles"))
    }

    /**
     * The next four lock in fail-closed behavior. A token the converter cannot read must grant
     * nothing: granting a default role, or throwing and letting a caller catch it, would both turn
     * a malformed token into access.
     */
    @Test
    fun `grants nothing when a path segment is missing`() {
        assertEquals(
            emptyList(),
            KeycloakJwtUtils.extractRoles(jwt(mapOf("realm_access" to emptyMap<String, Any>())), "realm_access.roles"),
        )
    }

    @Test
    fun `grants nothing when the claim is absent entirely`() {
        assertEquals(emptyList(), KeycloakJwtUtils.extractRoles(jwt(mapOf("sub" to "u1")), "realm_access.roles"))
    }

    @Test
    fun `grants nothing when the leaf is not a list`() {
        assertEquals(emptyList(), KeycloakJwtUtils.extractRoles(jwt(mapOf("roles" to "ADMIN")), "roles"))
    }

    @Test
    fun `keeps only the string entries of a mixed list`() {
        assertEquals(listOf("ADMIN"), KeycloakJwtUtils.extractRoles(jwt(mapOf("roles" to listOf("ADMIN", 42, null))), "roles"))
    }
}
