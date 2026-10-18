package com.vertyll.veds.notification.domain.model

import java.util.UUID

data class NotificationSearchCriteria(
    val recipientId: UUID,
    val onlyUnread: Boolean = false,
    val projectId: UUID? = null,
    val type: NotificationType? = null,
)
