@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.domain.service

import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPermission
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class TaskAccessPolicyTest {
    private val projectId = Uuid.generateV7().toJavaUuid()
    private val managerId = Uuid.generateV7().toJavaUuid()
    private val clientId = Uuid.generateV7().toJavaUuid()
    private val strangerId = Uuid.generateV7().toJavaUuid()

    private val manager =
        RolePermissionsRef(
            roleName = "MANAGER",
            permissions = TaskPermission.entries.mapTo(mutableSetOf()) { it.name },
        )
    private val client =
        RolePermissionsRef(
            roleName = "CLIENT",
            permissions = setOf(TaskPermission.VIEW_TASKS.name, TaskPermission.COMMENT.name),
        )
    private val unknownRole = RolePermissionsRef(roleName = "AUDITOR")

    private fun project(isActive: Boolean = true) = ProjectRef(projectId = projectId, name = "Apollo", isActive = isActive)

    private fun membership(
        userId: UUID,
        roleCode: String,
    ) = ProjectMembershipRef(projectId = projectId, userId = userId, roleCode = roleCode)

    private fun task(
        isActive: Boolean = true,
        accessRoleId: UUID? = null,
        createdBy: UUID = managerId,
        assignees: Set<UUID> = emptySet(),
    ) = Task(
        projectId = projectId,
        number = 1,
        name = "Write the thing",
        createdBy = createdBy,
        isActive = isActive,
        accessRoleId = accessRoleId,
        assigneeIds = assignees,
    )

    @Nested
    inner class RoleMapping {
        @Test
        fun `a role granted everything may do everything`() {
            val member = membership(managerId, "MANAGER")
            TaskPermission.entries.forEach { permission ->
                assertTrue(
                    TaskAccessPolicy.evaluate(project(), member, manager, permission).isPermitted,
                    "manager should hold $permission",
                )
            }
        }

        @Test
        fun `a role holds exactly what it was granted`() {
            val member = membership(clientId, "CLIENT")

            assertTrue(TaskAccessPolicy.evaluate(project(), member, client, TaskPermission.VIEW_TASKS).isPermitted)
            assertTrue(TaskAccessPolicy.evaluate(project(), member, client, TaskPermission.COMMENT).isPermitted)
            assertFalse(TaskAccessPolicy.evaluate(project(), member, client, TaskPermission.MANAGE_TASKS).isPermitted)
        }

        @Test
        fun `an unrestricted role holds a permission it was never granted`() {
            val administrator = RolePermissionsRef(roleName = "ADMIN", unrestricted = true)
            val member = membership(managerId, "ADMIN")

            TaskPermission.entries.forEach { permission ->
                assertTrue(TaskAccessPolicy.evaluate(project(), member, administrator, permission).isPermitted)
            }
        }

        @Test
        fun `a role the projection has never heard of grants nothing`() {
            val member = membership(clientId, "AUDITOR")

            TaskPermission.entries.forEach { permission ->
                assertFalse(TaskAccessPolicy.evaluate(project(), member, null, permission).isPermitted)
                assertFalse(TaskAccessPolicy.evaluate(project(), member, unknownRole, permission).isPermitted)
            }
        }

        @Test
        fun `a non-member gets nothing, even holding a role that grants everything`() {
            TaskPermission.entries.forEach { permission ->
                assertFalse(TaskAccessPolicy.evaluate(project(), null, manager, permission).isPermitted)
            }
        }
    }

    @Nested
    inner class ResourceState {
        @Test
        fun `an archived project freezes writes for every role`() {
            val member = membership(managerId, "MANAGER")
            val decision =
                TaskAccessPolicy.evaluate(project(isActive = false), member, manager, TaskPermission.MANAGE_TASKS)

            assertIs<AccessDecision.Deny>(decision)
            assertEquals(TaskError.PROJECT_ARCHIVED, decision.reason)
        }

        @Test
        fun `an archived project still allows reading`() {
            val member = membership(managerId, "MANAGER")

            assertTrue(
                TaskAccessPolicy
                    .evaluate(project(isActive = false), member, manager, TaskPermission.VIEW_TASKS)
                    .isPermitted,
            )
        }

        @Test
        fun `an archived task cannot be changed`() {
            val member = membership(managerId, "MANAGER")
            val decision =
                TaskAccessPolicy.evaluate(project(), member, manager, TaskPermission.MANAGE_TASKS, task(isActive = false))

            assertIs<AccessDecision.Deny>(decision)
            assertEquals(TaskError.TASK_ARCHIVED, decision.reason)
        }
    }

    @Nested
    inner class RestrictedTasks {
        private val restriction = Uuid.generateV7().toJavaUuid()

        @Test
        fun `an unrestricted task is visible to any member`() {
            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(task(), client, clientId))
        }

        @Test
        fun `the author always sees their own restricted task`() {
            val restricted = task(accessRoleId = restriction, createdBy = clientId)

            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(restricted, client, clientId))
        }

        @Test
        fun `an assignee sees a restricted task they were given`() {
            val restricted = task(accessRoleId = restriction, assignees = setOf(clientId))

            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(restricted, client, clientId))
        }

        @Test
        fun `a role without the restricted grant does not see a restricted task`() {
            val restricted = task(accessRoleId = restriction)

            assertFalse(TaskAccessPolicy.canSeeRestrictedTask(restricted, client, clientId))
        }

        @Test
        fun `a role holding the restricted grant sees it`() {
            val restricted = task(accessRoleId = restriction)

            assertTrue(TaskAccessPolicy.canSeeRestrictedTask(restricted, manager, managerId))
        }

        @Test
        fun `a non-member does not see a restricted task`() {
            val restricted = task(accessRoleId = restriction)

            assertFalse(TaskAccessPolicy.canSeeRestrictedTask(restricted, null, strangerId))
        }
    }

    @Test
    fun `permissionsOf agrees with evaluate for every role`() {
        listOf(manager, client, unknownRole, null).forEach { role ->
            val member = role?.let { membership(clientId, it.roleName) }
            val reported = TaskAccessPolicy.permissionsOf(member, role)
            val evaluated =
                TaskPermission.entries
                    .filter { TaskAccessPolicy.evaluate(project(), member, role, it).isPermitted }
                    .toSet()

            assertEquals(evaluated, reported, "role ${role?.roleName}")
        }
    }
}
