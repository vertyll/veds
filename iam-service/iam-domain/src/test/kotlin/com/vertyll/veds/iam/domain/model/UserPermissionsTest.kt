package com.vertyll.veds.iam.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UserPermissionsTest {
    private val read = "USERS_VIEW"
    private val write = "USERS_MANAGE"

    private val viewer = Role(id = 1, name = "viewer", permissions = setOf(read))
    private val admin = Role(id = 2, name = "admin", permissions = setOf(read, write))
    private val everyRole = listOf(viewer, admin)

    private fun user(vararg roles: Role) =
        User(id = 1, email = "a@example.com", firstName = "John", lastName = "Doe", roles = roles.mapTo(mutableSetOf()) { it.name })

    @Test
    fun `a user with no roles has no permissions`() {
        assertTrue(user().permissionsFrom(everyRole).isEmpty())
    }

    @Test
    fun `permissions are the union of every role`() {
        assertEquals(setOf(read, write), user(viewer, admin).permissionsFrom(everyRole))
    }

    @Test
    fun `overlapping roles do not duplicate a permission`() {
        assertEquals(2, user(viewer, admin).permissionsFrom(everyRole).size)
    }

    @Test
    fun `losing a role loses the permissions it granted`() {
        val stripped = user(viewer, admin).withoutRole(admin.name)

        assertEquals(setOf(read), stripped.permissionsFrom(everyRole))
    }

    @Test
    fun `a role the user does not hold grants nothing`() {
        assertEquals(setOf(read), user(viewer).permissionsFrom(everyRole))
    }

    @Test
    fun `holding a role twice changes nothing`() {
        val holder = user(viewer)

        assertSame(holder, holder.withRole(viewer.name))
    }
}
