@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.InMemoryRolePermissions
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.InMemoryUserDirectory
import com.vertyll.veds.task.application.InMemoryWorkLogRepository
import com.vertyll.veds.task.application.command.LogWorkCommand
import com.vertyll.veds.task.application.command.UpdateWorkLogCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.projectRef
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.application.task
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.WorkLogEntry
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class WorkLogCommandServiceTest {
    private val entries = InMemoryWorkLogRepository()
    private val tasks = InMemoryTaskRepository()
    private val directory = InMemoryProjectDirectory()
    private val users = InMemoryUserDirectory()

    private val roles = InMemoryRolePermissions()

    private val service =
        WorkLogCommandService(
            entryRepository = entries,
            taskRepository = tasks,
            userDirectory = users,
            projectDirectory = directory,
            authorization = TaskAuthorizationService(directory, tasks, roles),
        )

    private val project = projectRef().also { directory.saveProject(it) }
    private val projectId = project.projectId

    private fun actorWithRole(roleCode: String): Actor {
        val id = Uuid.generateV7().toJavaUuid()
        directory.saveMembership(membership(projectId, id, roleCode))
        return Actor(id = id, email = "$roleCode@example.com", firstName = "Test", lastName = "User")
    }

    private val author = actorWithRole("MEMBER")
    private val existingTask = task(projectId, createdBy = author.id).also { tasks.given(it) }

    private val day = LocalDate.of(2026, 9, 1)

    @Test
    fun `an entry is stored against its task, author and day`() {
        val response = service.logWork(existingTask.id, LogWorkCommand(90, day, "Pair session"), author)

        val stored = entries.findById(response.id)!!
        assertEquals(existingTask.id, stored.taskId)
        assertEquals(author.id, stored.authorId)
        assertEquals(90, stored.minutes)
        assertEquals(day, stored.workedOn)
        assertEquals("Pair session", stored.description)
    }

    @Test
    fun `the task total is the sum of its entries`() {
        service.logWork(existingTask.id, LogWorkCommand(90, day), author)
        service.logWork(existingTask.id, LogWorkCommand(45, day.plusDays(1)), author)

        assertEquals(135, tasks.findById(existingTask.id)!!.workedMinutes)
    }

    @Test
    fun `a blank description is stored as none`() {
        val response = service.logWork(existingTask.id, LogWorkCommand(30, day, null), author)

        assertNull(entries.findById(response.id)!!.description)
    }

    @Test
    fun `zero minutes is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            service.logWork(existingTask.id, LogWorkCommand(0, day), author)
        }
    }

    @Test
    fun `more than a day in one entry is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            service.logWork(existingTask.id, LogWorkCommand(WorkLogEntry.MAX_MINUTES_PER_ENTRY + 1, day), author)
        }
    }

    @Test
    fun `editing an entry refreshes the task total`() {
        val response = service.logWork(existingTask.id, LogWorkCommand(90, day), author)

        service.editEntry(response.id, UpdateWorkLogCommand(30, day, null), author, version = null)

        assertEquals(30, tasks.findById(existingTask.id)!!.workedMinutes)
    }

    @Test
    fun `somebody else's entry cannot be edited`() {
        val response = service.logWork(existingTask.id, LogWorkCommand(90, day), author)
        val other = actorWithRole("MEMBER")

        val error =
            assertFailsWith<ApiException> {
                service.editEntry(response.id, UpdateWorkLogCommand(30, day, null), other, version = null)
            }

        assertEquals(TaskError.WORK_LOG_NOT_AUTHORED_BY_CALLER, error.error)
        assertEquals(90, entries.findById(response.id)!!.minutes)
    }

    @Test
    fun `an unknown entry is reported as missing`() {
        val error =
            assertFailsWith<ApiException> {
                service.editEntry(
                    Uuid.generateV7().toJavaUuid(),
                    UpdateWorkLogCommand(30, day, null),
                    author,
                    version = null,
                )
            }

        assertEquals(TaskError.WORK_LOG_NOT_FOUND, error.error)
    }

    @Test
    fun `an edit against a stale version is refused`() {
        val response = service.logWork(existingTask.id, LogWorkCommand(90, day), author)

        val error =
            assertFailsWith<ApiException> {
                service.editEntry(response.id, UpdateWorkLogCommand(30, day, null), author, version = 99L)
            }

        assertEquals(TaskError.VERSION_MISMATCH, error.error)
    }

    @Test
    fun `deleting an entry takes its minutes off the task`() {
        val kept = service.logWork(existingTask.id, LogWorkCommand(60, day), author)
        val removed = service.logWork(existingTask.id, LogWorkCommand(90, day), author)

        service.deleteEntry(removed.id, author)

        assertNull(entries.findById(removed.id))
        assertEquals(60, entries.findById(kept.id)!!.minutes)
        assertEquals(60, tasks.findById(existingTask.id)!!.workedMinutes)
    }

    @Test
    fun `somebody else's entry cannot be deleted`() {
        val response = service.logWork(existingTask.id, LogWorkCommand(90, day), author)
        val other = actorWithRole("MEMBER")

        assertFailsWith<ApiException> { service.deleteEntry(response.id, other) }

        assertEquals(90, entries.findById(response.id)!!.minutes)
    }
}
