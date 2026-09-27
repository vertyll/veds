package com.vertyll.veds.task.domain.model

import java.util.UUID

data class TaskSearchCriteria(
    val projectId: UUID,
    val searchTerm: String? = null,
    val statusId: UUID? = null,
    val categoryId: UUID? = null,
    val assigneeId: UUID? = null,
    val priority: TaskPriority? = null,
    val onlyActive: Boolean = true,
    val sortBy: TaskSortField = TaskSortField.CREATED_AT,
    val sortDescending: Boolean = true,
)
