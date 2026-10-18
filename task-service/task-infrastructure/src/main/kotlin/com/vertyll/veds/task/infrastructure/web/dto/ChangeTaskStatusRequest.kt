package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.ChangeTaskStatusCommand
import java.util.UUID

data class ChangeTaskStatusRequest(
    val statusId: UUID? = null,
) {
    fun toCommand(): ChangeTaskStatusCommand = ChangeTaskStatusCommand(statusId = statusId)
}
