@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.InMemoryCommentRepository
import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.InMemoryRolePermissions
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.InMemoryUserDirectory
import com.vertyll.veds.task.application.RecordingTaskEventPublisher
import com.vertyll.veds.task.application.command.BatchDeleteTasksCommand
import com.vertyll.veds.task.application.command.ChangeTaskStatusCommand
import com.vertyll.veds.task.application.command.CreateTaskCommand
import com.vertyll.veds.task.application.command.UpdateTaskCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.projectRef
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.application.service.TaskReferenceValidator
import com.vertyll.veds.task.application.statusRef
import com.vertyll.veds.task.application.task
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.TaskComment
import com.vertyll.veds.task.domain.model.TaskPriority
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TaskCommandServiceTest {
    private val tasks = InMemoryTaskRepository()
    private val comments = InMemoryCommentRepository()
    private val directory = InMemoryProjectDirectory()
    private val users = InMemoryUserDirectory()
    private val events = RecordingTaskEventPublisher()

    private val roles = InMemoryRolePermissions()

    private val service =
        TaskCommandService(
            taskRepository = tasks,
            commentRepository = comments,
            userDirectory = users,
            authorization = TaskAuthorizationService(directory, tasks, roles),
            references = TaskReferenceValidator(directory),
            eventPublisher = events,
        )

    private val project = projectRef().also { directory.saveProject(it) }
    private val projectId = project.projectId

    private val actor = Actor(id = Uuid.generateV7().toJavaUuid(), email = "ada@example.com", firstName = "Ada", lastName = "Lovelace")

    init {
        directory.saveMembership(membership(projectId, actor.id, roleCode = "MANAGER"))
    }

    private val todo = statusRef(projectId, name = "To do").also { directory.saveStatus(it) }
    private val done = statusRef(projectId, name = "Done").also { directory.saveStatus(it) }

    private fun createCommand(
        statusId: UUID? = null,
        assigneeIds: Set<UUID> = emptySet(),
    ) = CreateTaskCommand(
        projectId = projectId,
        name = FIX_THE_THING,
        description = null,
        priority = TaskPriority.MEDIUM,
        statusId = statusId,
        categoryIds = emptySet(),
        assigneeIds = assigneeIds,
        priceEstimation = 0,
        accessRoleId = null,
        attachmentIds = emptySet(),
    )

    private fun updateCommand(
        name: String = FIX_THE_THING,
        statusId: UUID? = null,
        assigneeIds: Set<UUID> = emptySet(),
        priority: TaskPriority = TaskPriority.MEDIUM,
    ) = UpdateTaskCommand(
        name = name,
        description = null,
        priority = priority,
        statusId = statusId,
        categoryIds = emptySet(),
        assigneeIds = assigneeIds,
        priceEstimation = 0,
        accessRoleId = null,
        attachmentIds = emptySet(),
    )

    @Test
    fun `a task is stored against its project and creator`() {
        val response = service.createTask(createCommand(), actor)

        val stored = tasks.findById(response.id)!!
        assertEquals(projectId, stored.projectId)
        assertEquals(actor.id, stored.createdBy)
        assertEquals(FIX_THE_THING, stored.name)
    }

    @Test
    fun `creating a task announces it`() {
        val response = service.createTask(createCommand(), actor)

        assertEquals(listOf("TaskCreated(${response.id})"), events.published)
    }

    @Test
    fun `the creator is recorded in the user directory`() {
        service.createTask(createCommand(), actor)

        assertNotNull(users.findById(actor.id))
    }

    @Test
    fun `a status from another project cannot be used`() {
        val elsewhere = statusRef(Uuid.generateV7().toJavaUuid()).also { directory.saveStatus(it) }

        val error = assertFailsWith<ApiException> { service.createTask(createCommand(statusId = elsewhere.statusId), actor) }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
        assertTrue(tasks.stored.isEmpty())
    }

    @Test
    fun `someone who is not a member cannot create a task`() {
        val outsider = Actor(Uuid.generateV7().toJavaUuid(), "out@example.com", null, null)

        assertFailsWith<ApiException> { service.createTask(createCommand(), outsider) }

        assertTrue(tasks.stored.isEmpty())
        assertTrue(events.published.isEmpty())
    }

    private fun givenTask(
        statusId: UUID? = null,
        assigneeIds: Set<UUID> = emptySet(),
    ) = task(projectId, createdBy = actor.id, statusId = statusId, assigneeIds = assigneeIds).also { tasks.given(it) }

    @Test
    fun `updating replaces the editable fields`() {
        val existing = givenTask()

        service.updateTask(existing.id, updateCommand(name = "Fix it properly", priority = TaskPriority.HIGH), actor, 0L)

        val stored = tasks.findById(existing.id)!!
        assertEquals("Fix it properly", stored.name)
        assertEquals(TaskPriority.HIGH, stored.priority)
    }

    @Test
    fun `gaining an assignee is announced`() {
        val existing = givenTask()
        val assignee = membership(projectId).also { directory.saveMembership(it) }

        service.updateTask(existing.id, updateCommand(assigneeIds = setOf(assignee.userId)), actor, 0L)

        assertEquals(listOf("TaskAssigned(${existing.id},1)"), events.published)
    }

    @Test
    fun `an edit that does not touch the assignees announces nothing`() {
        val assignee = membership(projectId).also { directory.saveMembership(it) }
        val existing = givenTask(assigneeIds = setOf(assignee.userId))

        service.updateTask(existing.id, updateCommand(name = "Reworded", assigneeIds = setOf(assignee.userId)), actor, 0L)

        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `moving a task to another status is announced`() {
        val existing = givenTask(statusId = todo.statusId)

        service.updateTask(existing.id, updateCommand(statusId = done.statusId), actor, 0L)

        assertEquals(listOf("TaskStatusChanged(${existing.id},${done.statusId})"), events.published)
    }

    @Test
    fun `an update against a stale version is refused`() {
        val existing = givenTask()

        val error = assertFailsWith<ApiException> { service.updateTask(existing.id, updateCommand(name = "New"), actor, 9L) }

        assertEquals(TaskError.VERSION_MISMATCH, error.error)
        assertEquals(FIX_THE_THING, tasks.findById(existing.id)!!.name)
    }

    @Test
    fun `an assignee who is not a member is refused`() {
        val existing = givenTask()

        val error =
            assertFailsWith<ApiException> {
                service.updateTask(
                    existing.id,
                    updateCommand(assigneeIds = setOf(Uuid.generateV7().toJavaUuid())),
                    actor,
                    0L,
                )
            }

        assertEquals(TaskError.ASSIGNEE_NOT_A_MEMBER, error.error)
    }

    @Test
    fun `changing the status moves the task and announces it`() {
        val existing = givenTask(statusId = todo.statusId)

        service.changeStatus(existing.id, ChangeTaskStatusCommand(done.statusId), actor, 0L)

        assertEquals(done.statusId, tasks.findById(existing.id)!!.statusId)
        assertEquals(listOf("TaskStatusChanged(${existing.id},${done.statusId})"), events.published)
    }

    @Test
    fun `dropping a task back into its own column announces nothing`() {
        val existing = givenTask(statusId = todo.statusId)

        service.changeStatus(existing.id, ChangeTaskStatusCommand(todo.statusId), actor, 0L)

        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `a status from another project cannot be moved to`() {
        val existing = givenTask(statusId = todo.statusId)
        val elsewhere = statusRef(Uuid.generateV7().toJavaUuid()).also { directory.saveStatus(it) }

        assertFailsWith<ApiException> { service.changeStatus(existing.id, ChangeTaskStatusCommand(elsewhere.statusId), actor, 0L) }

        assertEquals(todo.statusId, tasks.findById(existing.id)!!.statusId)
    }

    @Test
    fun `archiving deactivates the task, drops its comments and announces it`() {
        val existing = givenTask()
        comments.given(TaskComment(taskId = existing.id, authorId = actor.id, content = "Working on it"))

        service.archiveTask(existing.id, actor, 0L)

        assertTrue(!tasks.findById(existing.id)!!.isActive)
        assertTrue(comments.findAllByTaskId(existing.id).isEmpty())
        assertEquals(listOf("TaskArchived(${existing.id})"), events.published)
    }

    @Test
    fun `archiving an already archived task is refused`() {
        val archived = task(projectId, createdBy = actor.id).archive().also { tasks.given(it) }

        val error = assertFailsWith<ApiException> { service.archiveTask(archived.id, actor, 0L) }

        assertEquals(TaskError.TASK_ARCHIVED, error.error)
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `archiving against a stale version is refused`() {
        val existing = givenTask()

        assertFailsWith<ApiException> { service.archiveTask(existing.id, actor, 9L) }

        assertTrue(tasks.findById(existing.id)!!.isActive)
    }

    @Test
    fun `a batch archives every task and announces each one`() {
        val first = givenTask()
        val second = givenTask()

        val archived = service.archiveTasks(BatchDeleteTasksCommand(setOf(first.id, second.id)), actor)

        assertEquals(2, archived)
        assertTrue(tasks.stored.values.none { it.isActive })
        assertEquals(2, events.published.count { it.startsWith("TaskArchived") })
    }

    @Test
    fun `an empty batch does nothing`() {
        assertEquals(0, service.archiveTasks(BatchDeleteTasksCommand(emptySet()), actor))
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `a batch containing an unreachable task archives nothing`() {
        val mine = givenTask()
        val elsewhere = task(Uuid.generateV7().toJavaUuid()).also { tasks.given(it) }

        assertFailsWith<ApiException> { service.archiveTasks(BatchDeleteTasksCommand(setOf(mine.id, elsewhere.id)), actor) }

        assertTrue(tasks.findById(mine.id)!!.isActive)
        assertTrue(events.published.isEmpty())
    }
}

private const val FIX_THE_THING = "Fix the thing"
