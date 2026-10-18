package com.vertyll.veds.notification.application.dto

import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationType
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val type: NotificationType,
    val messageKey: String,
    val params: Map<String, String>,
    val projectId: UUID?,
    val subjectId: UUID?,
    val isRead: Boolean,
    val readAt: Instant?,
    val createdAt: Instant,
    val version: Long?,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse =
            NotificationResponse(
                id = notification.id,
                type = notification.type,
                messageKey = notification.type.key,
                params = notification.params,
                projectId = notification.projectId,
                subjectId = notification.subjectId,
                isRead = notification.isRead,
                readAt = notification.readAt,
                createdAt = notification.createdAt,
                version = notification.version,
            )
    }
}
