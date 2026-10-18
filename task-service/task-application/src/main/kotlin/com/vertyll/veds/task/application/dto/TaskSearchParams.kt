package com.vertyll.veds.task.application.dto

import com.vertyll.veds.task.domain.model.TaskPriority
import com.vertyll.veds.task.domain.model.TaskSortField
import java.util.UUID

data class TaskSearchParams(
    val searchTerm: String? = null,
    val statusId: UUID? = null,
    val categoryId: UUID? = null,
    val assigneeId: UUID? = null,
    val priority: TaskPriority? = null,
    val onlyActive: Boolean = true,
    val sortBy: TaskSortField = TaskSortField.CREATED_AT,
    val sortDescending: Boolean = true,
    val page: Int = 0,
    val size: Int = DEFAULT_PAGE_SIZE,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 25
    }
}
