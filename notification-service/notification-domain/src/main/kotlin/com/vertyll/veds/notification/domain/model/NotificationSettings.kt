package com.vertyll.veds.notification.domain.model

import java.util.UUID

data class NotificationSettings(
    val userId: UUID,
    val mutedTypes: Set<NotificationType> = emptySet(),
    val emailEnabledTypes: Set<NotificationType> = DEFAULT_EMAIL_TYPES,
    val version: Long? = null,
) {
    fun allows(
        type: NotificationType,
        channel: NotificationChannel,
    ): Boolean =
        when (channel) {
            NotificationChannel.IN_APP -> type !in mutedTypes
            NotificationChannel.EMAIL -> type !in mutedTypes && type in emailEnabledTypes
        }

    fun mute(type: NotificationType): NotificationSettings = copy(mutedTypes = mutedTypes + type)

    fun unmute(type: NotificationType): NotificationSettings = copy(mutedTypes = mutedTypes - type)

    fun enableEmail(type: NotificationType): NotificationSettings = copy(emailEnabledTypes = emailEnabledTypes + type)

    companion object {
        val DEFAULT_EMAIL_TYPES: Set<NotificationType> =
            setOf(NotificationType.PROJECT_INVITATION, NotificationType.TASK_ASSIGNED)

        fun defaultFor(userId: UUID): NotificationSettings = NotificationSettings(userId = userId)
    }
}
