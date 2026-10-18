package com.vertyll.veds.task.application.dto

import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPriority
import java.time.Instant
import java.util.UUID

data class TaskResponse(
    val id: UUID,
    val projectId: UUID,
    val number: Int,
    val name: String,
    val description: String?,
    val priority: TaskPriority,
    val priceEstimation: Int,
    val workedMinutes: Int,
    val statusId: UUID?,
    val categoryIds: Set<UUID>,
    val assigneeIds: Set<UUID>,
    val accessRoleId: UUID?,
    val attachmentIds: Set<UUID>,
    val createdBy: UUID,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
) {
    companion object {
        fun from(task: Task): TaskResponse =
            TaskResponse(
                id = task.id,
                projectId = task.projectId,
                number = task.number,
                name = task.name,
                description = task.description,
                priority = task.priority,
                priceEstimation = task.priceEstimation,
                workedMinutes = task.workedMinutes,
                statusId = task.statusId,
                categoryIds = task.categoryIds,
                assigneeIds = task.assigneeIds,
                accessRoleId = task.accessRoleId,
                attachmentIds = task.attachmentIds,
                createdBy = task.createdBy,
                isActive = task.isActive,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                version = task.version,
            )
    }
}
