package com.vertyll.veds.iam.application.service.command

import com.vertyll.veds.iam.application.FakeIdentityProvider
import com.vertyll.veds.iam.application.InMemoryPermissionRepository
import com.vertyll.veds.iam.application.InMemoryRoleRepository
import com.vertyll.veds.iam.application.InMemoryUserRepository
import com.vertyll.veds.iam.application.RecordingRolePermissionsPublisher
import com.vertyll.veds.iam.application.command.CreateRoleCommand
import com.vertyll.veds.iam.application.command.UpdateRoleCommand
import com.vertyll.veds.iam.application.permission
import com.vertyll.veds.iam.application.role
import com.vertyll.veds.iam.application.user
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class RoleCommandServiceTest {
    private val roles = InMemoryRoleRepository()
    private val users = InMemoryUserRepository()
    private val identity = FakeIdentityProvider()

    private val permissions = InMemoryPermissionRepository()
    private val events = RecordingRolePermissionsPublisher()

    private val service = RoleCommandService(roles, permissions, users, identity, events)

    private val adminRole = role(id = 2L, name = "ADMIN").also { roles.given(it) }

    private fun givenUser(
        id: Long = 1L,
        vararg granted: com.vertyll.veds.iam.domain.model.Role,
    ) = user(id = id, roles = granted.toSet()).copy(version = 0L).also { users.given(it) }

    @Test
    fun `granting a role records it locally and in the identity provider`() {
        val existing = givenUser()

        service.assignRoleToUser(existing.id!!, "ADMIN", version = 0L)

        assertTrue(users.findById(existing.id!!)!!.roles.contains(adminRole))
        assertTrue(identity.calls.contains("assignRole(${existing.keycloakId},ADMIN)"))
    }

    @Test
    fun `revoking a role removes it from both`() {
        val existing = givenUser(1L, adminRole)

        service.removeRoleFromUser(existing.id!!, "ADMIN", version = 0L)

        assertTrue(users.findById(existing.id!!)!!.roles.isEmpty())
        assertTrue(identity.calls.contains("removeRole(${existing.keycloakId},ADMIN)"))
    }

    @Test
    fun `a role that does not exist cannot be granted`() {
        val existing = givenUser()

        val error = assertFailsWith<ApiException> { service.assignRoleToUser(existing.id!!, "NOT_A_ROLE", version = 0L) }

        assertEquals(IamError.ROLE_NOT_FOUND, error.error)
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `an unknown user cannot be granted a role`() {
        val error = assertFailsWith<ApiException> { service.assignRoleToUser(404L, "ADMIN", version = 0L) }

        assertEquals(IamError.USER_NOT_FOUND, error.error)
    }

    @Test
    fun `a grant against a stale version is refused`() {
        val existing = givenUser()

        val error = assertFailsWith<ApiException> { service.assignRoleToUser(existing.id!!, "ADMIN", version = 9L) }

        assertEquals(IamError.VERSION_MISMATCH, error.error)
        assertTrue(users.findById(existing.id!!)!!.roles.isEmpty())
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `a revocation against a stale version is refused`() {
        val existing = givenUser(1L, adminRole)

        assertFailsWith<ApiException> { service.removeRoleFromUser(existing.id!!, "ADMIN", version = 9L) }

        assertTrue(users.findById(existing.id!!)!!.roles.contains(adminRole))
        assertTrue(identity.calls.isEmpty())
    }

    @Test
    fun `granting a role the user already holds changes nothing`() {
        val existing = givenUser(1L, adminRole)

        service.assignRoleToUser(existing.id!!, "ADMIN", version = 0L)

        assertEquals(setOf(adminRole), users.findById(existing.id!!)!!.roles)
    }

    @Test
    fun `creating a role grants the permissions it names and announces them`() {
        permissions.given(permission(id = 1L, name = "TASKS_VIEW"))

        val created =
            service.createRole(
                CreateRoleCommand(
                    name = "lead",
                    description = "Team lead",
                    permissions = setOf("TASKS_VIEW"),
                    scope = RoleScope.PROJECT,
                ),
            )

        assertEquals("LEAD", created.name)
        assertEquals(listOf("TASKS_VIEW"), created.permissions)
        assertEquals(
            listOf("TASKS_VIEW"),
            events.changed
                .single()
                .permissions
                .map { it.name },
        )
    }

    @Test
    fun `creating a role that names an unknown permission is refused`() {
        val failure =
            assertFailsWith<ApiException> {
                service.createRole(CreateRoleCommand(name = "LEAD", description = null, permissions = setOf("NOPE")))
            }

        assertEquals(IamError.PERMISSION_NOT_FOUND, failure.error)
        assertTrue(roles.stored.none { it.name == "LEAD" })
    }

    @Test
    fun `a second role under an existing name is refused`() {
        val failure =
            assertFailsWith<ApiException> {
                service.createRole(CreateRoleCommand(name = "ADMIN", description = null, permissions = emptySet()))
            }

        assertEquals(IamError.ROLE_ALREADY_EXISTS, failure.error)
    }

    @Test
    fun `updating a role replaces what it grants`() {
        val view = permission(id = 1L, name = "TASKS_VIEW")
        val manage = permission(id = 2L, name = "TASKS_MANAGE")
        permissions.given(view, manage)
        roles.given(role(id = 7L, name = "LEAD", permissions = setOf(view)).copy(version = 0L, scope = RoleScope.PROJECT))

        val updated =
            service.updateRole(
                name = "LEAD",
                command = UpdateRoleCommand(description = "Team lead", permissions = setOf("TASKS_MANAGE")),
                version = 0L,
            )

        assertEquals(listOf("TASKS_MANAGE"), updated.permissions)
        assertEquals(
            listOf("TASKS_MANAGE"),
            events.changed
                .single()
                .permissions
                .map { it.name },
        )
    }

    @Test
    fun `updating a role against a stale version is refused`() {
        roles.given(role(id = 7L, name = "LEAD").copy(version = 3L))

        val failure =
            assertFailsWith<ApiException> {
                service.updateRole("LEAD", UpdateRoleCommand(null, emptySet()), version = 1L)
            }

        assertEquals(IamError.VERSION_MISMATCH, failure.error)
    }

    @Test
    fun `deleting a role announces its removal`() {
        roles.given(role(id = 7L, name = "LEAD"))

        service.deleteRole("LEAD")

        assertTrue(roles.stored.none { it.name == "LEAD" })
        assertEquals(listOf("LEAD"), events.removed)
        assertTrue(identity.calls.contains("deleteRole(LEAD)"))
    }

    @Test
    fun `a role someone still holds cannot be deleted`() {
        val lead = role(id = 7L, name = "LEAD").also { roles.given(it) }
        givenUser(1L, lead)

        val failure = assertFailsWith<ApiException> { service.deleteRole("LEAD") }

        assertEquals(IamError.ROLE_STILL_ASSIGNED, failure.error)
        assertTrue(roles.stored.any { it.name == "LEAD" })
    }

    @Test
    fun `a role the platform ships cannot be deleted`() {
        val failure = assertFailsWith<ApiException> { service.deleteRole("ADMIN") }

        assertEquals(IamError.ROLE_IS_SYSTEM, failure.error)
    }

    @Test
    fun `a project role is stored in its scope and never reaches the identity provider`() {
        val created =
            service.createRole(
                CreateRoleCommand(
                    name = "AUDITOR",
                    description = null,
                    permissions = emptySet(),
                    scope = RoleScope.PROJECT,
                ),
            )

        assertEquals(RoleScope.PROJECT.name, created.scope)
        assertEquals(RoleScope.PROJECT, roles.findByName("AUDITOR")?.scope)
        assertEquals(RoleScope.PROJECT, events.changed.single().scope)
        assertTrue(identity.calls.none { it.startsWith("createRole") })
    }

    @Test
    fun `a global role is announced to the identity provider`() {
        service.createRole(CreateRoleCommand(name = "AUDITOR", description = null, permissions = emptySet()))

        assertEquals(RoleScope.GLOBAL, roles.findByName("AUDITOR")?.scope)
        assertTrue(identity.calls.contains("createRole(AUDITOR)"))
    }

    @Test
    fun `a role cannot be granted a permission held in another scope`() {
        permissions.given(permission(id = 1L, name = "USERS_MANAGE", module = "admin", scope = RoleScope.GLOBAL))

        val failure =
            assertFailsWith<ApiException> {
                service.createRole(
                    CreateRoleCommand(
                        name = "LEAD",
                        description = null,
                        permissions = setOf("USERS_MANAGE"),
                        scope = RoleScope.PROJECT,
                    ),
                )
            }

        assertEquals(IamError.PERMISSION_OUT_OF_SCOPE, failure.error)
        assertTrue(roles.stored.none { it.name == "LEAD" })
    }

    @Test
    fun `the last person holding the last unrestricted role keeps it`() {
        val onlyAdmin = role(id = 9L, name = "ROOT", unrestricted = true).also { roles.given(it) }
        val holder = givenUser(1L, onlyAdmin)

        val failure =
            assertFailsWith<ApiException> { service.removeRoleFromUser(holder.id!!, "ROOT", holder.version) }

        assertEquals(IamError.LAST_UNRESTRICTED_ROLE, failure.error)
    }

    @Test
    fun `an unrestricted role can be taken away while somebody else still holds one`() {
        val first = role(id = 9L, name = "ROOT", unrestricted = true).also { roles.given(it) }
        val second = role(id = 10L, name = "ROOT_TWO", unrestricted = true).also { roles.given(it) }
        val holder = givenUser(1L, first)
        givenUser(2L, second)

        service.removeRoleFromUser(holder.id!!, "ROOT", holder.version)

        assertTrue(
            users.stored
                .getValue(holder.id!!)
                .roles
                .none { it.name == "ROOT" },
        )
    }

    @Test
    fun `an unrestricted role cannot be deleted`() {
        roles.given(role(id = 9L, name = "ROOT", unrestricted = true))

        val failure = assertFailsWith<ApiException> { service.deleteRole("ROOT") }

        assertEquals(IamError.LAST_UNRESTRICTED_ROLE, failure.error)
    }
}
