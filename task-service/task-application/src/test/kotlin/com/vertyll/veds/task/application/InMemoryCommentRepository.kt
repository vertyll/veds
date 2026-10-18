package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.TaskComment
import com.vertyll.veds.task.domain.repository.TaskCommentRepository
import java.util.UUID

internal class InMemoryCommentRepository : TaskCommentRepository {
    val stored = linkedMapOf<UUID, TaskComment>()

    fun given(vararg comments: TaskComment) = comments.forEach { stored[it.id] = it }

    override fun save(comment: TaskComment) = comment.also { stored[it.id] = it }

    override fun saveAll(comments: Collection<TaskComment>) = comments.map { save(it) }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByTaskId(taskId: UUID) = stored.values.filter { it.taskId == taskId }

    override fun findAllByAttachmentId(attachmentId: UUID) = stored.values.filter { attachmentId in it.attachmentIds }

    override fun delete(id: UUID) {
        stored.remove(id)
    }

    override fun deleteAllByTaskId(taskId: UUID) {
        stored.values.removeAll { it.taskId == taskId }
    }
}
