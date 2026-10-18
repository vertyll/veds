package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.UpdateTaskCommand
import com.vertyll.veds.task.domain.model.TaskPriority
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class UpdateTaskRequest(
    @field:NotBlank(message = "validation.task.description_required")
    val name: String = "",
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val statusId: UUID? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val assigneeIds: Set<UUID> = emptySet(),
    @field:Min(value = 0, message = "validation.task.estimation_negative")
    val priceEstimation: Int = 0,
    val accessRoleId: UUID? = null,
    val attachmentIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): UpdateTaskCommand =
        UpdateTaskCommand(
            name = name,
            description = description,
            priority = priority,
            statusId = statusId,
            categoryIds = categoryIds,
            assigneeIds = assigneeIds,
            priceEstimation = priceEstimation,
            accessRoleId = accessRoleId,
            attachmentIds = attachmentIds,
        )
}
