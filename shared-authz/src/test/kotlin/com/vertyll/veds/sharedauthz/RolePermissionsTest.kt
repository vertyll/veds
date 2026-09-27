package com.vertyll.veds.sharedauthz

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RolePermissionsTest {
    @Test
    fun `a role grants what it was given`() {
        val role = RolePermissions(role = "MEMBER", module = "task", permissions = setOf("VIEW_TASKS"))

        assertTrue(role.grants("VIEW_TASKS"))
        assertFalse(role.grants("MANAGE_TASKS"))
    }

    @Test
    fun `an unrestricted role grants a permission nobody listed`() {
        val role = RolePermissions(role = "ADMINISTRATOR", module = "task", permissions = emptySet(), unrestricted = true)

        assertTrue(role.grants("A_PERMISSION_ADDED_NEXT_YEAR"))
    }
}
