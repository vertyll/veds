package com.vertyll.veds.notification.application.port.inbound.query

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.dto.NotificationSettingsResponse
import com.vertyll.veds.notification.application.dto.PagedResponse
import com.vertyll.veds.notification.domain.model.NotificationType
import java.util.UUID

interface NotificationQueryUseCase {
    fun list(
        actorId: UUID,
        onlyUnread: Boolean,
        projectId: UUID?,
        type: NotificationType?,
        page: Int,
        size: Int,
    ): PagedResponse<NotificationResponse>

    fun unreadCount(actorId: UUID): Long

    fun getSettings(actorId: UUID): NotificationSettingsResponse
}
