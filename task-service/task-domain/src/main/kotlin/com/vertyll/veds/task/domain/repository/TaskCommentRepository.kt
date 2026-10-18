package com.vertyll.veds.task.domain.repository

import com.vertyll.veds.task.domain.model.TaskComment
import java.util.UUID

interface TaskCommentRepository {
    fun save(comment: TaskComment): TaskComment

    fun saveAll(comments: Collection<TaskComment>): List<TaskComment>

    fun findById(id: UUID): TaskComment?

    fun findAllByTaskId(taskId: UUID): List<TaskComment>

    fun findAllByAttachmentId(attachmentId: UUID): List<TaskComment>

    fun delete(id: UUID)

    fun deleteAllByTaskId(taskId: UUID)
}
