package com.vertyll.veds.notification.application.port.outbound

import com.vertyll.veds.notification.application.dto.NotificationResponse
import java.util.UUID

interface NotificationPushPort {
    fun push(
        recipientId: UUID,
        notification: NotificationResponse,
    )

    fun pushUnreadCount(
        recipientId: UUID,
        unread: Long,
    )
}
