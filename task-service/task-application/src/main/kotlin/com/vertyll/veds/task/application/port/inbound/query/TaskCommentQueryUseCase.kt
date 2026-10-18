package com.vertyll.veds.task.application.port.inbound.query

import com.vertyll.veds.task.application.dto.TaskCommentResponse
import java.util.UUID

@Suppress("kotlin:S6517")
interface TaskCommentQueryUseCase {
    fun getComments(
        taskId: UUID,
        actorId: UUID,
    ): List<TaskCommentResponse>
}
