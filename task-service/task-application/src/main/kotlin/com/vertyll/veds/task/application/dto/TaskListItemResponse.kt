package com.vertyll.veds.task.application.dto

import com.vertyll.veds.task.domain.model.TaskPriority
import java.time.Instant
import java.util.UUID

data class TaskListItemResponse(
    val id: UUID,
    val projectId: UUID,
    val number: Int,
    val name: String,
    val priority: TaskPriority,
    val statusId: UUID?,
    val statusName: String?,
    val statusColor: String?,
    val categories: List<TaskCategoryView>,
    val assignees: List<TaskUserView>,
    val commentCount: Int,
    val workedMinutes: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
)
