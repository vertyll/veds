package com.vertyll.veds.iam.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RoleTest {
    private val read = Permission(id = 1, name = "USERS_VIEW")
    private val write = Permission(id = 2, name = "USERS_MANAGE")

    @Test
    fun `a role reports what it grants`() {
        val role = Role(id = 1, name = "admin", permissions = setOf(read))

        assertTrue(role.grants("USERS_VIEW"))
        assertFalse(role.grants("USERS_MANAGE"))
    }

    @Test
    fun `adding a permission twice changes nothing`() {
        val role = Role(id = 1, name = "admin", permissions = setOf(read))
        val again = role.withPermission(read)

        assertSame(role, again)
        assertEquals(1, again.permissions.size)
    }

    @Test
    fun `adding and removing round-trips`() {
        val role = Role(id = 1, name = "admin").withPermission(read).withPermission(write)

        assertEquals(2, role.permissions.size)
        assertEquals(setOf(read), role.withoutPermission(write.id!!).permissions)
    }

    @Test
    fun `removing something the role never had is a no-op`() {
        val role = Role(id = 1, name = "admin", permissions = setOf(read))

        assertSame(role, role.withoutPermission(999))
    }
}
