package com.vertyll.veds.sharedauthz

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class StockRoleTest {
    @Test
    fun `a stock role keeps the permissions it grants`() {
        val catalogue =
            permissions("task") {
                permission("VIEW_TASKS", description = "See tasks")
                stockRole("TASK_VIEWER", RoleScope.PROJECT, "VIEW_TASKS")
            }

        assertEquals(setOf("VIEW_TASKS"), catalogue.stockRoles.single().permissions)
        assertEquals("See tasks", catalogue.definitions.single().description)
    }

    @Test
    fun `a stock role name must be upper snake case`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                StockRole(name = "task-viewer", scope = RoleScope.PROJECT, permissions = emptySet())
            }

        assertContains(error.message.orEmpty(), "task-viewer")
    }

    @Test
    fun `declaring the same stock role twice is refused`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                permissions("task") {
                    permission("VIEW_TASKS")
                    stockRole("TASK_VIEWER", RoleScope.PROJECT, "VIEW_TASKS")
                    stockRole("TASK_VIEWER", RoleScope.PROJECT, "VIEW_TASKS")
                }
            }

        assertContains(error.message.orEmpty(), "duplicate stock role")
    }

    @Test
    fun `a stock role cannot grant what the module does not declare`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                permissions("task") {
                    permission("VIEW_TASKS")
                    stockRole("TASK_VIEWER", RoleScope.PROJECT, "DELETE_THE_WORLD")
                }
            }

        assertContains(error.message.orEmpty(), "DELETE_THE_WORLD")
    }

    @Test
    fun `a stock role cannot grant a permission held in another scope`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                permissions("task") {
                    permission("VIEW_TASKS", scope = RoleScope.PROJECT)
                    stockRole("ADMIN", RoleScope.GLOBAL, "VIEW_TASKS")
                }
            }

        assertContains(error.message.orEmpty(), "another scope")
    }
}
