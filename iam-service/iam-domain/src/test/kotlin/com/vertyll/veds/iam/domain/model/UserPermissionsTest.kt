package com.vertyll.veds.iam.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserPermissionsTest {
    private val read = Permission(id = 1, name = "USERS_VIEW")
    private val write = Permission(id = 2, name = "USERS_MANAGE")

    private val viewer = Role(id = 1, name = "viewer", permissions = setOf(read))
    private val admin = Role(id = 2, name = "admin", permissions = setOf(read, write))

    private fun user(vararg roles: Role) =
        User(id = 1, email = "a@example.com", firstName = "John", lastName = "Doe", roles = roles.toSet())

    @Test
    fun `a user with no roles has no permissions`() {
        assertTrue(user().permissions.isEmpty())
    }

    @Test
    fun `permissions are the union of every role`() {
        assertEquals(setOf(read, write), user(viewer, admin).permissions)
    }

    @Test
    fun `overlapping roles do not duplicate a permission`() {
        assertEquals(2, user(viewer, admin).permissions.size)
    }

    @Test
    fun `losing a role loses the permissions it granted`() {
        val stripped = user(viewer, admin).withoutRole(admin.id!!)

        assertEquals(setOf(read), stripped.permissions)
    }
}
