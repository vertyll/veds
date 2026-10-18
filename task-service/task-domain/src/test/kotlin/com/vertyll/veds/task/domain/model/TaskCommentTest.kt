@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class TaskCommentTest {
    private val author = Uuid.generateV7().toJavaUuid()
    private val someoneElse = Uuid.generateV7().toJavaUuid()

    private fun comment() = TaskComment.create(taskId = Uuid.generateV7().toJavaUuid(), authorId = author, content = "Looks good")

    @Test
    fun `the author may edit`() {
        val edited = comment().editedBy(author, "Looks better")

        assertEquals("Looks better", edited.content)
    }

    @Test
    fun `nobody else may edit, even a manager`() {
        assertFailsWith<IllegalStateException> { comment().editedBy(someoneElse, "Hijacked") }
    }

    @Test
    fun `rejects blank content`() {
        assertFailsWith<IllegalArgumentException> {
            TaskComment.create(taskId = Uuid.generateV7().toJavaUuid(), authorId = author, content = "   ")
        }
    }

    @Test
    fun `recognises its author`() {
        assertTrue(comment().isAuthoredBy(author))
    }
}
