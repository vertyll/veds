package com.vertyll.veds.notification.infrastructure.web.dto

import com.vertyll.veds.notification.application.command.DismissNotificationsCommand
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class DismissNotificationsRequest(
    @field:NotEmpty(message = "validation.notification.ids_required")
    val notificationIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): DismissNotificationsCommand = DismissNotificationsCommand(notificationIds = notificationIds)
}
