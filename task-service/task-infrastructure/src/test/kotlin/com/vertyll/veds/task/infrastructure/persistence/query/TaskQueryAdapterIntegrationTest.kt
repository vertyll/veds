@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.infrastructure.persistence.query

import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.infrastructure.IntegrationTestBase
import com.vertyll.veds.task.infrastructure.persistence.entity.TaskJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.UserRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.TaskJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.UserRefJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TaskQueryAdapterIntegrationTest(
    private val taskQueries: TaskQueryPort,
    private val tasks: TaskJpaRepository,
    private val userRefs: UserRefJpaRepository,
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
}
