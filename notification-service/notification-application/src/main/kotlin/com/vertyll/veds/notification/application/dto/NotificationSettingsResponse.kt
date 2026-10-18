package com.vertyll.veds.notification.application.dto

import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.model.NotificationType

data class NotificationSettingsResponse(
    val mutedTypes: Set<NotificationType>,
    val emailEnabledTypes: Set<NotificationType>,
    val availableTypes: Set<NotificationType>,
    val version: Long?,
) {
    companion object {
        fun from(settings: NotificationSettings): NotificationSettingsResponse =
            NotificationSettingsResponse(
                mutedTypes = settings.mutedTypes,
                emailEnabledTypes = settings.emailEnabledTypes,
                availableTypes = NotificationType.entries.toSet(),
                version = settings.version,
            )
    }
}
