package com.vertyll.veds.notification.application.dto

import com.vertyll.veds.notification.domain.model.NotificationType
import java.util.UUID

data class NotificationSearchParams(
    val onlyUnread: Boolean = false,
    val projectId: UUID? = null,
    val type: NotificationType? = null,
)
