@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.InMemoryRolePermissions
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.projectRef
import com.vertyll.veds.task.application.task
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.TaskPermission
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TaskAuthorizationServiceTest {
    private val directory = InMemoryProjectDirectory()
    private val tasks = InMemoryTaskRepository()

    private val roles = InMemoryRolePermissions()

    private val service = TaskAuthorizationService(directory, tasks, roles)

    private val actor = Uuid.generateV7().toJavaUuid()
    private val project = projectRef().also { directory.saveProject(it) }
    private val projectId = project.projectId

    private fun givenRole(
        roleCode: String,
        userId: UUID = actor,
    ) {
        directory.saveMembership(membership(projectId, userId, roleCode))
    }

    @Test
    fun `a manager may manage tasks`() {
        givenRole("MANAGER")

        assertEquals(projectId, service.requireProjectPermission(projectId, actor, TaskPermission.MANAGE_TASKS).projectId)
    }

    @Test
    fun `a client may view and comment but not manage`() {
        givenRole("CLIENT")

        service.requireProjectPermission(projectId, actor, TaskPermission.VIEW_TASKS)
        service.requireProjectPermission(projectId, actor, TaskPermission.COMMENT)

        val error = assertFailsWith<ApiException> { service.requireProjectPermission(projectId, actor, TaskPermission.MANAGE_TASKS) }
        assertEquals(TaskError.TASK_ACCESS_DENIED, error.error)
    }

    @Test
    fun `someone with no membership is refused`() {
        val error = assertFailsWith<ApiException> { service.requireProjectPermission(projectId, actor, TaskPermission.VIEW_TASKS) }

        assertEquals(TaskError.TASK_ACCESS_DENIED, error.error)
    }

    @Test
    fun `an unmapped role grants nothing`() {
        givenRole("AUDITOR")

        assertTrue(service.effectivePermissions(projectId, actor).isEmpty())
        assertFailsWith<ApiException> { service.requireProjectPermission(projectId, actor, TaskPermission.VIEW_TASKS) }
    }

    @Test
    fun `a project this service has not heard about grants nothing`() {
        val error =
            assertFailsWith<ApiException> {
                service.requireProjectPermission(
                    Uuid.generateV7().toJavaUuid(),
                    actor,
                    TaskPermission.VIEW_TASKS,
                )
            }

        assertEquals(TaskError.PROJECT_NOT_KNOWN, error.error)
    }

    @Test
    fun `an archived project is read-only`() {
        directory.saveProject(project.copy(isActive = false))
        givenRole("MANAGER")

        service.requireProjectPermission(projectId, actor, TaskPermission.VIEW_TASKS)

        val error = assertFailsWith<ApiException> { service.requireProjectPermission(projectId, actor, TaskPermission.MANAGE_TASKS) }
        assertEquals(TaskError.PROJECT_ARCHIVED, error.error)
    }

    @Test
    fun `a member may open a task on their own board`() {
        givenRole("MEMBER")
        val existing = task(projectId).also { tasks.given(it) }

        assertEquals(existing.id, service.requireTaskPermission(existing.id, actor, TaskPermission.VIEW_TASKS).id)
    }

    @Test
    fun `an unknown task is reported as missing`() {
        givenRole("MEMBER")

        val error =
            assertFailsWith<ApiException> {
                service.requireTaskPermission(
                    Uuid.generateV7().toJavaUuid(),
                    actor,
                    TaskPermission.VIEW_TASKS,
                )
            }

        assertEquals(TaskError.TASK_NOT_FOUND, error.error)
    }

    @Test
    fun `a task on a board the caller has no access to looks missing`() {
        val existing = task(projectId).also { tasks.given(it) }

        val error = assertFailsWith<ApiException> { service.requireTaskPermission(existing.id, actor, TaskPermission.VIEW_TASKS) }

        assertEquals(TaskError.TASK_NOT_FOUND, error.error)
    }

    @Test
    fun `a restricted task is invisible to an ordinary member`() {
        givenRole("MEMBER")
        val restricted = task(projectId, accessRoleId = Uuid.generateV7().toJavaUuid()).also { tasks.given(it) }

        val error = assertFailsWith<ApiException> { service.requireTaskPermission(restricted.id, actor, TaskPermission.VIEW_TASKS) }

        assertEquals(TaskError.TASK_NOT_FOUND, error.error)
    }

    @Test
    fun `the author of a restricted task can still see it`() {
        givenRole("MEMBER")
        val restricted = task(projectId, createdBy = actor, accessRoleId = Uuid.generateV7().toJavaUuid()).also { tasks.given(it) }

        assertEquals(restricted.id, service.requireTaskPermission(restricted.id, actor, TaskPermission.VIEW_TASKS).id)
    }

    @Test
    fun `an assignee of a restricted task can see it`() {
        givenRole("MEMBER")
        val restricted = task(projectId, assigneeIds = setOf(actor), accessRoleId = Uuid.generateV7().toJavaUuid()).also { tasks.given(it) }

        assertEquals(restricted.id, service.requireTaskPermission(restricted.id, actor, TaskPermission.VIEW_TASKS).id)
    }

    @Test
    fun `a manager can see any restricted task on their board`() {
        givenRole("MANAGER")
        val restricted = task(projectId, accessRoleId = Uuid.generateV7().toJavaUuid()).also { tasks.given(it) }

        assertEquals(restricted.id, service.requireTaskPermission(restricted.id, actor, TaskPermission.VIEW_TASKS).id)
    }

    @Test
    fun `an archived task cannot be changed but can still be read`() {
        givenRole("MANAGER")
        val archived = task(projectId).archive().also { tasks.given(it) }

        service.requireTaskPermission(archived.id, actor, TaskPermission.VIEW_TASKS)

        val error = assertFailsWith<ApiException> { service.requireTaskPermission(archived.id, actor, TaskPermission.MANAGE_TASKS) }
        assertEquals(TaskError.TASK_ARCHIVED, error.error)
    }

    @Test
    fun `a manager's effective permissions cover everything`() {
        givenRole("MANAGER")

        assertEquals(TaskPermission.entries.toSet(), service.effectivePermissions(projectId, actor))
    }

    @Test
    fun `a client's effective permissions exclude managing`() {
        givenRole("CLIENT")

        assertEquals(setOf(TaskPermission.VIEW_TASKS, TaskPermission.COMMENT), service.effectivePermissions(projectId, actor))
    }

    @Test
    fun `a non-member has no effective permissions`() {
        assertTrue(service.effectivePermissions(projectId, actor).isEmpty())
    }
}
