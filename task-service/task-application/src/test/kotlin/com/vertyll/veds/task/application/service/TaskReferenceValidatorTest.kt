@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.categoryRef
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.statusRef
import com.vertyll.veds.task.domain.error.TaskError
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TaskReferenceValidatorTest {
    private val directory = InMemoryProjectDirectory()
    private val validator = TaskReferenceValidator(directory)

    private val projectId = Uuid.generateV7().toJavaUuid()
    private val otherProjectId = Uuid.generateV7().toJavaUuid()

    private fun validate(
        statusId: UUID? = null,
        categoryIds: Set<UUID> = emptySet(),
        assigneeIds: Set<UUID> = emptySet(),
    ) = validator.validate(projectId, statusId, categoryIds, assigneeIds)

    @Test
    fun `a task naming nothing passes`() {
        validate()
    }

    @Test
    fun `a status belonging to the project is accepted`() {
        val status = statusRef(projectId).also { directory.saveStatus(it) }

        validate(statusId = status.statusId)
    }

    @Test
    fun `an unknown status is refused`() {
        val error = assertFailsWith<ApiException> { validate(statusId = Uuid.generateV7().toJavaUuid()) }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
    }

    @Test
    fun `a status belonging to another project is refused`() {
        val elsewhere = statusRef(otherProjectId).also { directory.saveStatus(it) }

        val error = assertFailsWith<ApiException> { validate(statusId = elsewhere.statusId) }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
    }

    @Test
    fun `categories belonging to the project are accepted`() {
        val first = categoryRef(projectId).also { directory.saveCategory(it) }
        val second = categoryRef(projectId).also { directory.saveCategory(it) }

        validate(categoryIds = setOf(first.categoryId, second.categoryId))
    }

    @Test
    fun `an unknown category is refused and named`() {
        val known = categoryRef(projectId).also { directory.saveCategory(it) }
        val unknown = Uuid.generateV7().toJavaUuid()

        val error = assertFailsWith<ApiException> { validate(categoryIds = setOf(known.categoryId, unknown)) }

        assertEquals(TaskError.CATEGORY_NOT_IN_PROJECT, error.error)
        assertEquals(listOf(unknown.toString()), error.params["categoryIds"])
    }

    @Test
    fun `a category belonging to another project is refused`() {
        val elsewhere = categoryRef(otherProjectId).also { directory.saveCategory(it) }

        assertFailsWith<ApiException> { validate(categoryIds = setOf(elsewhere.categoryId)) }
    }

    @Test
    fun `members of the project can be assigned`() {
        val member = membership(projectId).also { directory.saveMembership(it) }

        validate(assigneeIds = setOf(member.userId))
    }

    @Test
    fun `someone who is not a member cannot be assigned`() {
        val outsider = Uuid.generateV7().toJavaUuid()

        val error = assertFailsWith<ApiException> { validate(assigneeIds = setOf(outsider)) }

        assertEquals(TaskError.ASSIGNEE_NOT_A_MEMBER, error.error)
        assertEquals(listOf(outsider.toString()), error.params["userIds"])
    }

    @Test
    fun `a member of another project cannot be assigned`() {
        val elsewhere = membership(otherProjectId).also { directory.saveMembership(it) }

        assertFailsWith<ApiException> { validate(assigneeIds = setOf(elsewhere.userId)) }
    }

    @Test
    fun `a bad status is reported before a bad assignee`() {
        val error =
            assertFailsWith<ApiException> {
                validate(
                    statusId = Uuid.generateV7().toJavaUuid(),
                    assigneeIds = setOf(Uuid.generateV7().toJavaUuid()),
                )
            }

        assertEquals(TaskError.STATUS_NOT_IN_PROJECT, error.error)
    }
}
