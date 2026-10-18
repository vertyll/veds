package com.vertyll.veds.notification.application

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.port.outbound.NotificationPushPort
import java.util.UUID

internal class RecordingPush : NotificationPushPort {
    val pushed = mutableListOf<String>()
    val unreadCounts = mutableListOf<Pair<UUID, Long>>()

    override fun push(
        recipientId: UUID,
        notification: NotificationResponse,
    ) {
        pushed += "push($recipientId,${notification.type})"
    }

    override fun pushUnreadCount(
        recipientId: UUID,
        unread: Long,
    ) {
        unreadCounts += recipientId to unread
    }
}
