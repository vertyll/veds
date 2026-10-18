package com.vertyll.veds.task.application.command

import com.vertyll.veds.task.domain.model.TaskPriority
import java.util.UUID

data class UpdateTaskCommand(
    val name: String,
    val description: String?,
    val priority: TaskPriority,
    val statusId: UUID?,
    val categoryIds: Set<UUID>,
    val assigneeIds: Set<UUID>,
    val priceEstimation: Int,
    val accessRoleId: UUID?,
    val attachmentIds: Set<UUID>,
)
