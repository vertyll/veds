package com.vertyll.veds.task.application.port.outbound

import java.util.UUID

interface TaskEventPublisherPort {
    fun publishTaskCreated(
        taskId: UUID,
        projectId: UUID,
        name: String,
        createdBy: UUID,
        assigneeIds: Set<UUID>,
    )

    fun publishTaskAssigned(
        taskId: UUID,
        projectId: UUID,
        assigneeIds: Set<UUID>,
        assignedBy: UUID,
    )

    fun publishTaskStatusChanged(
        taskId: UUID,
        projectId: UUID,
        statusId: UUID?,
        changedBy: UUID,
    )

    fun publishTaskArchived(
        taskId: UUID,
        projectId: UUID,
    )

    fun publishCommentAdded(
        taskId: UUID,
        projectId: UUID,
        commentId: UUID,
        authorId: UUID,
        excerpt: String,
    )
}
