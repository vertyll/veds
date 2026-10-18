package com.vertyll.veds.task.application.port.inbound.query

import com.vertyll.veds.task.application.dto.PagedResponse
import com.vertyll.veds.task.application.dto.TaskDetailsResponse
import com.vertyll.veds.task.application.dto.TaskListItemResponse
import com.vertyll.veds.task.application.dto.TaskSearchParams
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.TaskPermission
import java.util.UUID

interface TaskQueryUseCase {
    fun searchTasks(
        projectId: UUID,
        params: TaskSearchParams,
        actorId: UUID,
        language: LanguageTag,
    ): PagedResponse<TaskListItemResponse>

    fun getTaskDetails(
        taskId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): TaskDetailsResponse

    fun getEffectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<TaskPermission>
}
