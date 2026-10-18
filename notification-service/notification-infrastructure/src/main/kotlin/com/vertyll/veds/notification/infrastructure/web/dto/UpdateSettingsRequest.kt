package com.vertyll.veds.notification.infrastructure.web.dto

import com.vertyll.veds.notification.application.command.UpdateSettingsCommand
import com.vertyll.veds.notification.domain.model.NotificationType

data class UpdateSettingsRequest(
    val mutedTypes: Set<NotificationType> = emptySet(),
    val emailEnabledTypes: Set<NotificationType> = emptySet(),
) {
    fun toCommand(): UpdateSettingsCommand = UpdateSettingsCommand(mutedTypes = mutedTypes, emailEnabledTypes = emailEnabledTypes)
}
