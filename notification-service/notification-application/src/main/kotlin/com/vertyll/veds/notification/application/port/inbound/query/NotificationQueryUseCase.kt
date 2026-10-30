package com.vertyll.veds.notification.application.port.inbound.query

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.dto.NotificationSearchParams
import com.vertyll.veds.notification.application.dto.NotificationSettingsResponse
import com.vertyll.veds.notification.application.dto.PagedResponse
import java.util.UUID

interface NotificationQueryUseCase {
    fun list(
        params: NotificationSearchParams,
        actorId: UUID,
        page: Int,
        size: Int,
    ): PagedResponse<NotificationResponse>

    fun unreadCount(actorId: UUID): Long

    fun getSettings(actorId: UUID): NotificationSettingsResponse
}
