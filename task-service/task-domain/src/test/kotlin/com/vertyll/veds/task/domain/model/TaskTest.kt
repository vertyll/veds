@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class TaskTest {
    private val creator = Uuid.generateV7().toJavaUuid()
    private val statusId = Uuid.generateV7().toJavaUuid()

    private fun task() = Task(projectId = Uuid.generateV7().toJavaUuid(), number = 1, name = "Write the thing", createdBy = creator)

    @Test
    fun `moving to the same status returns the very same instance`() {
        val original = task()
        assertSame(original, original.moveTo(null))

        val withStatus = original.moveTo(statusId)
        assertSame(withStatus, withStatus.moveTo(statusId))
    }

    @Test
    fun `moving to a different status produces a new state`() {
        val original = task()
        val moved = original.moveTo(statusId)

        assertEquals(statusId, moved.statusId)
        assertEquals(original.id, moved.id)
    }

    @Test
    fun `rejects a blank description`() {
        assertFailsWith<IllegalArgumentException> {
            Task(projectId = Uuid.generateV7().toJavaUuid(), number = 1, name = "  ", createdBy = creator)
        }
    }

    @Test
    fun `rejects negative money and time`() {
        assertFailsWith<IllegalArgumentException> { task().estimateAt(-1) }
        assertFailsWith<IllegalArgumentException> { task().withWorkedMinutes(-1) }
    }

    @Test
    fun `worked minutes are replaced, not accumulated`() {
        val worked = task().withWorkedMinutes(150).withWorkedMinutes(200)

        assertEquals(200, worked.workedMinutes)
    }

    @Test
    fun `archiving keeps the record and can be undone`() {
        val original = task()
        val archived = original.archive()

        assertFalse(archived.isActive)
        assertTrue(archived.restore().isActive)
        assertEquals(original.id, archived.restore().id)
    }

    @Test
    fun `dropping a category is idempotent`() {
        val categoryId = Uuid.generateV7().toJavaUuid()
        val categorised = task().categoriseAs(setOf(categoryId))

        assertEquals(emptySet(), categorised.withoutCategory(categoryId).categoryIds)
        assertSame(categorised, categorised.withoutCategory(Uuid.generateV7().toJavaUuid()))
    }

    @Test
    fun `clearing a status only affects the task that had it`() {
        val moved = task().moveTo(statusId)

        assertEquals(null, moved.withoutStatus(statusId).statusId)
        assertSame(moved, moved.withoutStatus(Uuid.generateV7().toJavaUuid()))
    }

    @Test
    fun `dropping an attachment is idempotent`() {
        val fileId = Uuid.generateV7().toJavaUuid()
        val withFile = task().withAttachments(setOf(fileId))

        assertEquals(emptySet(), withFile.withoutAttachment(fileId).attachmentIds)
        assertSame(withFile, withFile.withoutAttachment(Uuid.generateV7().toJavaUuid()))
    }
}
