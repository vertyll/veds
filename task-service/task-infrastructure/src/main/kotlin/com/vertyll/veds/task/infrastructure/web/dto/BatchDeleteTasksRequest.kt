package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.BatchDeleteTasksCommand
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class BatchDeleteTasksRequest(
    @field:NotEmpty(message = "validation.task.batch_empty")
    val taskIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): BatchDeleteTasksCommand = BatchDeleteTasksCommand(taskIds = taskIds)
}
