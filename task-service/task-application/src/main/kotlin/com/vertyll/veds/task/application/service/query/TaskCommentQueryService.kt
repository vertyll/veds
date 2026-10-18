package com.vertyll.veds.task.application.service.query

import com.vertyll.veds.task.application.dto.TaskCommentResponse
import com.vertyll.veds.task.application.port.inbound.query.TaskCommentQueryUseCase
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.domain.model.TaskPermission
import java.util.UUID

class TaskCommentQueryService(
    private val queryPort: TaskQueryPort,
    private val authorization: TaskAuthorizationService,
) : TaskCommentQueryUseCase {
    override fun getComments(
        taskId: UUID,
        actorId: UUID,
    ): List<TaskCommentResponse> {
        authorization.requireTaskPermission(taskId, actorId, TaskPermission.VIEW_TASKS)
        return queryPort.findComments(taskId)
    }
}
