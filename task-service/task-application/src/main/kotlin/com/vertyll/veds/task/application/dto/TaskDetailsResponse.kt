package com.vertyll.veds.task.application.dto

import com.vertyll.veds.task.domain.model.TaskPermission

data class TaskDetailsResponse(
    val task: TaskResponse,
    val statusName: String?,
    val categories: List<TaskCategoryView>,
    val assignees: List<TaskUserView>,
    val createdBy: TaskUserView?,
    val comments: List<TaskCommentResponse>,
    val permissions: Set<TaskPermission>,
    val hiddenWorkLogEnabled: Boolean,
)
