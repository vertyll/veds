package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.InMemoryPermissionRepository
import com.vertyll.veds.iam.application.InMemoryRoleRepository
import com.vertyll.veds.iam.application.RecordingRolePermissionsPublisher
import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand
import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand.PermissionDeclaration
import com.vertyll.veds.iam.application.command.RegisterPermissionCatalogueCommand.StockRoleDeclaration
import com.vertyll.veds.iam.application.permission
import com.vertyll.veds.iam.application.role
import com.vertyll.veds.iam.domain.model.RoleScope
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class PermissionCatalogueServiceTest {
    private val permissions = InMemoryPermissionRepository()
    private val roles = InMemoryRoleRepository()
    private val events = RecordingRolePermissionsPublisher()

    private val service = PermissionCatalogueService(permissions, roles, events)

    private fun register(vararg declared: PermissionDeclaration) =
        service.register(RegisterPermissionCatalogueCommand(module = "task", permissions = declared.toList()))

    @Test
    fun `a permission a module declares for the first time is stored under that module`() {
        register(PermissionDeclaration("TASKS_VIEW", "See tasks", RoleScope.PROJECT))

        val stored = permissions.findByName("TASKS_VIEW")

        assertEquals("task", stored?.module)
        assertEquals("See tasks", stored?.description)
    }

    @Test
    fun `re-registering the same catalogue leaves one row per permission`() {
        register(PermissionDeclaration("TASKS_VIEW", "See tasks", RoleScope.PROJECT))
        register(PermissionDeclaration("TASKS_VIEW", "See tasks", RoleScope.PROJECT))

        assertEquals(1, permissions.findByModule("task").size)
    }

    @Test
    fun `a changed description reaches the stored permission`() {
        register(PermissionDeclaration("TASKS_VIEW", "See tasks", RoleScope.PROJECT))
        register(PermissionDeclaration("TASKS_VIEW", "Read the task board", RoleScope.PROJECT))

        assertEquals("Read the task board", permissions.findByName("TASKS_VIEW")?.description)
    }

    @Test
    fun `a permission the module no longer declares is withdrawn`() {
        permissions.given(permission(id = 1L, name = "TASKS_ARCHIVE"))

        register(PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT))

        assertNull(permissions.findByName("TASKS_ARCHIVE"))
    }

    @Test
    fun `withdrawing a permission announces the roles that granted it`() {
        val withdrawn = permission(id = 1L, name = "TASKS_ARCHIVE")
        permissions.given(withdrawn)
        roles.given(role(id = 5L, name = "LEAD", permissions = setOf(withdrawn)))

        register(PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT))

        val withdrawnFrom = events.changed.first { it.name == "LEAD" }

        assertTrue(withdrawnFrom.permissions.isEmpty())
    }

    @Test
    fun `another module's permissions are left alone`() {
        permissions.given(permission(id = 1L, name = "USERS_VIEW", module = "admin"))

        register(PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT))

        assertEquals(listOf("USERS_VIEW"), permissions.findByModule("admin").map { it.name })
    }

    @Test
    fun `registering announces what every role grants, so a module that just joined can hear it`() {
        roles.given(role(id = 5L, name = "LEAD"))

        register(PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT))

        assertTrue(events.changed.any { it.name == "LEAD" })
    }

    @Test
    fun `a stock role is created with everything the module names`() {
        service.register(
            RegisterPermissionCatalogueCommand(
                module = "task",
                permissions =
                    listOf(
                        PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT),
                        PermissionDeclaration("TASKS_MANAGE", null, RoleScope.PROJECT),
                    ),
                stockRoles =
                    listOf(
                        StockRoleDeclaration("MANAGER", RoleScope.PROJECT, setOf("TASKS_VIEW", "TASKS_MANAGE")),
                    ),
            ),
        )

        val manager = roles.findByName("MANAGER")

        assertEquals(RoleScope.PROJECT, manager?.scope)
        assertEquals(setOf("TASKS_VIEW", "TASKS_MANAGE"), manager!!.permissions.mapTo(mutableSetOf()) { it.name })
    }

    @Test
    fun `a permission the registry has never seen reaches an existing stock role`() {
        roles.given(role(id = 5L, name = "MANAGER"))

        service.register(
            RegisterPermissionCatalogueCommand(
                module = "task",
                permissions = listOf(PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT)),
                stockRoles = listOf(StockRoleDeclaration("MANAGER", RoleScope.PROJECT, setOf("TASKS_VIEW"))),
            ),
        )

        assertEquals(setOf("TASKS_VIEW"), roles.findByName("MANAGER")!!.permissions.mapTo(mutableSetOf()) { it.name })
    }

    @Test
    fun `redeploying a module does not restore a permission an administrator took away`() {
        val declaration =
            RegisterPermissionCatalogueCommand(
                module = "task",
                permissions = listOf(PermissionDeclaration("TASKS_VIEW", null, RoleScope.PROJECT)),
                stockRoles = listOf(StockRoleDeclaration("MANAGER", RoleScope.PROJECT, setOf("TASKS_VIEW"))),
            )
        service.register(declaration)
        val stripped = roles.findByName("MANAGER")!!.withPermissions(emptySet())
        roles.save(stripped)

        service.register(declaration)

        assertTrue(roles.findByName("MANAGER")!!.permissions.isEmpty())
    }
}
