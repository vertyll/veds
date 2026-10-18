package com.vertyll.veds.task.application.port.outbound

import com.vertyll.veds.task.application.dto.TaskCommentResponse
import com.vertyll.veds.task.application.dto.TaskListItemResponse
import com.vertyll.veds.task.application.dto.WorkLogPageResponse
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.model.WorkLogVisibility
import java.util.UUID

interface TaskQueryPort {
    fun searchTasks(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
        language: LanguageTag,
    ): PageResult<TaskListItemResponse>

    fun findComments(taskId: UUID): List<TaskCommentResponse>

    fun findWorkLog(
        taskId: UUID,
        readerId: UUID,
        readsHidden: Boolean,
        visibility: WorkLogVisibility,
        pageRequest: PageRequest,
    ): WorkLogPageResponse
}
