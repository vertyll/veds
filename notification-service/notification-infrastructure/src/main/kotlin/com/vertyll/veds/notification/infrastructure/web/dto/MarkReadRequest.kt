package com.vertyll.veds.notification.infrastructure.web.dto

import com.vertyll.veds.notification.application.command.MarkReadCommand
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class MarkReadRequest(
    @field:NotEmpty(message = "validation.notification.ids_required")
    val notificationIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): MarkReadCommand = MarkReadCommand(notificationIds = notificationIds)
}
