package com.vertyll.veds.notification.application.command

import com.vertyll.veds.notification.domain.model.NotificationType

data class UpdateSettingsCommand(
    val mutedTypes: Set<NotificationType>,
    val emailEnabledTypes: Set<NotificationType>,
)
