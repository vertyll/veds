@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.InMemoryCommentRepository
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.SilentLogger
import com.vertyll.veds.task.application.task
import com.vertyll.veds.task.domain.model.TaskComment
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class FileProjectionServiceTest {
    private val tasks = InMemoryTaskRepository()
    private val comments = InMemoryCommentRepository()

    private val service = FileProjectionService(tasks, comments, SilentLogger)

    private val projectId = Uuid.generateV7().toJavaUuid()
    private val deleted = Uuid.generateV7().toJavaUuid()

    private fun comment(vararg attachmentIds: UUID) =
        TaskComment(
            taskId = Uuid.generateV7().toJavaUuid(),
            authorId = Uuid.generateV7().toJavaUuid(),
            content = "See the attachment",
            attachmentIds = attachmentIds.toSet(),
        ).also { comments.given(it) }

    @Test
    fun `a deleted file is dropped from the tasks that attached it`() {
        val kept = Uuid.generateV7().toJavaUuid()
        val attached = task(projectId, attachmentIds = setOf(deleted, kept)).also { tasks.given(it) }

        service.fileDeleted(deleted)

        assertEquals(setOf(kept), tasks.findById(attached.id)!!.attachmentIds)
    }

    @Test
    fun `a deleted file is dropped from the comments that attached it`() {
        val attached = comment(deleted)

        service.fileDeleted(deleted)

        assertTrue(comments.findById(attached.id)!!.attachmentIds.isEmpty())
    }

    @Test
    fun `the same file is dropped from tasks and comments in one pass`() {
        val attachedTask = task(projectId, attachmentIds = setOf(deleted)).also { tasks.given(it) }
        val attachedComment = comment(deleted)

        service.fileDeleted(deleted)

        assertTrue(tasks.findById(attachedTask.id)!!.attachmentIds.isEmpty())
        assertTrue(comments.findById(attachedComment.id)!!.attachmentIds.isEmpty())
    }

    @Test
    fun `deleting the same file twice is harmless`() {
        val attached = task(projectId, attachmentIds = setOf(deleted)).also { tasks.given(it) }

        service.fileDeleted(deleted)
        service.fileDeleted(deleted)

        assertTrue(tasks.findById(attached.id)!!.attachmentIds.isEmpty())
    }

    @Test
    fun `a file nothing references is ignored`() {
        val untouched = task(projectId, attachmentIds = setOf(Uuid.generateV7().toJavaUuid())).also { tasks.given(it) }

        service.fileDeleted(deleted)

        assertEquals(untouched, tasks.findById(untouched.id))
    }
}
