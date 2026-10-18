package com.vertyll.veds.task.application.service.query

import com.vertyll.veds.task.application.dto.WorkLogPageResponse
import com.vertyll.veds.task.application.port.inbound.query.WorkLogQueryUseCase
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.WorkLogVisibility
import java.util.UUID

class WorkLogQueryService(
    private val queryPort: TaskQueryPort,
    private val authorization: TaskAuthorizationService,
) : WorkLogQueryUseCase {
    override fun getEntries(
        taskId: UUID,
        actorId: UUID,
        visibility: WorkLogVisibility,
        pageRequest: PageRequest,
    ): WorkLogPageResponse {
        val task = authorization.requireTaskPermission(taskId, actorId, TaskPermission.VIEW_TASKS)
        val readsHidden =
            authorization.effectivePermissions(task.projectId, actorId).contains(TaskPermission.VIEW_HIDDEN_WORK_LOG)

        return queryPort.findWorkLog(taskId, actorId, readsHidden, visibility, pageRequest)
    }
}
