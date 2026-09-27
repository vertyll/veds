package com.vertyll.veds.task.application

import com.vertyll.veds.task.application.port.outbound.TaskEventPublisherPort
import java.util.UUID

internal class RecordingTaskEventPublisher : TaskEventPublisherPort {
    val published = mutableListOf<String>()

    override fun publishTaskCreated(
        taskId: UUID,
        projectId: UUID,
        name: String,
        createdBy: UUID,
        assigneeIds: Set<UUID>,
    ) {
        published += "TaskCreated($taskId)"
    }

    override fun publishTaskAssigned(
        taskId: UUID,
        projectId: UUID,
        assigneeIds: Set<UUID>,
        assignedBy: UUID,
    ) {
        published += "TaskAssigned($taskId,${assigneeIds.size})"
    }

    override fun publishTaskStatusChanged(
        taskId: UUID,
        projectId: UUID,
        statusId: UUID?,
        changedBy: UUID,
    ) {
        published += "TaskStatusChanged($taskId,$statusId)"
    }

    override fun publishTaskArchived(
        taskId: UUID,
        projectId: UUID,
    ) {
        published += "TaskArchived($taskId)"
    }

    override fun publishCommentAdded(
        taskId: UUID,
        projectId: UUID,
        commentId: UUID,
        authorId: UUID,
        excerpt: String,
    ) {
        published += "CommentAdded($taskId,$commentId)"
    }
}
