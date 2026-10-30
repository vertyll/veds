@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.infrastructure.persistence.query

import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.model.WorkLogVisibility
import com.vertyll.veds.task.infrastructure.IntegrationTestBase
import com.vertyll.veds.task.infrastructure.persistence.entity.TaskCommentJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.TaskJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.UserRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.WorkLogEntryJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.TaskCommentJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.TaskJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.UserRefJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.WorkLogEntryJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TaskQueryAdapterIntegrationTest(
    private val taskQueries: TaskQueryPort,
    private val tasks: TaskJpaRepository,
    private val userRefs: UserRefJpaRepository,
    private val comments: TaskCommentJpaRepository,
    private val workLog: WorkLogEntryJpaRepository,
) : IntegrationTestBase() {
    private val projectId: UUID = Uuid.generateV7().toJavaUuid()
    private val assigneeId: UUID = Uuid.generateV7().toJavaUuid()

    @BeforeEach
    fun resetSharedState() {
        tasks.deleteAll(tasks.findAll().filter { it.projectId == projectId })
        userRefs.findById(assigneeId).ifPresent { userRefs.delete(it) }
    }

    @Test
    fun `a task list resolves its assignees from the user directory`() {
        userRefs.save(
            UserRefJpaEntity(
                userId = assigneeId,
                email = "assignee@example.com",
                firstName = "Ada",
                lastName = "Lovelace",
            ),
        )
        tasks.save(
            TaskJpaEntity(
                id = Uuid.generateV7().toJavaUuid(),
                projectId = projectId,
                number = 1,
                name = "assigned task",
                createdBy = assigneeId,
                assigneeIds = mutableSetOf(assigneeId),
            ),
        )

        val page =
            taskQueries.searchTasks(
                TaskSearchCriteria(projectId = projectId),
                PageRequest(page = 0, size = 25),
                LanguageTag("pl"),
            )

        assertEquals(1, page.content.size)
        assertEquals(
            listOf("Ada Lovelace"),
            page.content
                .single()
                .assignees
                .map { it.displayName },
        )
    }

    private fun savedTask(): UUID {
        val taskId = Uuid.generateV7().toJavaUuid()
        tasks.save(
            TaskJpaEntity(
                id = taskId,
                projectId = projectId,
                number = 1,
                name = "logged task",
                createdBy = assigneeId,
            ),
        )
        return taskId
    }

    private fun logged(
        taskId: UUID,
        authorId: UUID,
        minutes: Int,
        hidden: Boolean = false,
    ) = workLog.save(
        WorkLogEntryJpaEntity(
            id = Uuid.generateV7().toJavaUuid(),
            taskId = taskId,
            authorId = authorId,
            minutes = minutes,
            workedOn = LocalDate.parse("2026-01-01"),
            hidden = hidden,
        ),
    )

    @Test
    fun `a comment names its author from the user directory`() {
        val author = Uuid.generateV7().toJavaUuid()
        userRefs.save(UserRefJpaEntity(userId = author, email = "author@example.com", firstName = "Ada", lastName = "Lovelace"))
        val taskId = savedTask()
        comments.save(
            TaskCommentJpaEntity(id = Uuid.generateV7().toJavaUuid(), taskId = taskId, authorId = author, content = "Looks good"),
        )

        val found = taskQueries.findComments(taskId).single()

        assertEquals("Looks good", found.content)
        assertEquals("Ada Lovelace", found.author.displayName)
    }

    @Test
    fun `someone who may not read hidden entries sees the visible ones and their own`() {
        val reader = Uuid.generateV7().toJavaUuid()
        val colleague = Uuid.generateV7().toJavaUuid()
        val taskId = savedTask()
        logged(taskId, colleague, minutes = 30)
        logged(taskId, reader, minutes = 45, hidden = true)
        logged(taskId, colleague, minutes = 60, hidden = true)

        val page =
            taskQueries.findWorkLog(
                taskId,
                reader,
                readsHidden = false,
                visibility = WorkLogVisibility.ALL,
                pageRequest = PageRequest(page = 0, size = 25),
            )

        assertEquals(2, page.totalElements)
        assertEquals(75, page.totalMinutes)
        assertEquals(setOf(30, 45), page.content.map { it.minutes }.toSet())
    }

    @Test
    fun `someone who may read hidden entries can ask for only those`() {
        val reader = Uuid.generateV7().toJavaUuid()
        val taskId = savedTask()
        logged(taskId, reader, minutes = 30)
        logged(taskId, reader, minutes = 60, hidden = true)

        val page =
            taskQueries.findWorkLog(
                taskId,
                reader,
                readsHidden = true,
                visibility = WorkLogVisibility.HIDDEN,
                pageRequest = PageRequest(page = 0, size = 25),
            )

        assertEquals(1, page.totalElements)
        assertEquals(60, page.totalMinutes)
        assertEquals(true, page.content.single().hidden)
    }
}
