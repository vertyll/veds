package com.vertyll.veds.shared.web.security

import com.vertyll.veds.sharedauthz.RolePermissionsSource
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionAuthorizerTest {
    private class Projection(
        private val granted: Map<String, Set<String>> = emptyMap(),
        private val unrestricted: Set<String> = emptySet(),
    ) : RolePermissionsSource {
        override fun forRoles(roles: Collection<String>): Set<String> = roles.flatMapTo(mutableSetOf()) { granted[it].orEmpty() }

        override fun isUnrestricted(roles: Collection<String>): Boolean = roles.any { it in unrestricted }
    }

    private fun signInWith(vararg roles: String) {
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "n/a", authorities)
    }

    @AfterTest
    fun signOut() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `a role grants what the projection says it grants`() {
        signInWith("EDITOR")
        val authorizer = PermissionAuthorizer(Projection(mapOf("EDITOR" to setOf("EDIT"))))

        assertTrue(authorizer.has("EDIT"))
        assertFalse(authorizer.has("DELETE"))
    }

    @Test
    fun `an unrestricted role holds every permission`() {
        signInWith("ROOT")

        assertTrue(PermissionAuthorizer(Projection(unrestricted = setOf("ROOT"))).has("ANYTHING"))
    }

    @Test
    fun `a caller without roles holds nothing`() {
        assertFalse(PermissionAuthorizer(Projection(unrestricted = setOf("ROOT"))).has("EDIT"))
    }

    @Test
    fun `a service without a projection fails closed`() {
        signInWith("ROOT")

        assertFalse(PermissionAuthorizer(null).has("EDIT"))
    }
}
