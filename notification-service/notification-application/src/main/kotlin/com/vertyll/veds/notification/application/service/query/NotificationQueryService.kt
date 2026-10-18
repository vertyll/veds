package com.vertyll.veds.notification.application.service.query

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.dto.NotificationSettingsResponse
import com.vertyll.veds.notification.application.dto.PagedResponse
import com.vertyll.veds.notification.application.dto.PaginationMeta
import com.vertyll.veds.notification.application.port.inbound.query.NotificationQueryUseCase
import com.vertyll.veds.notification.domain.model.NotificationSearchCriteria
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.domain.model.PageRequest
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import java.util.UUID

class NotificationQueryService(
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: NotificationSettingsRepository,
) : NotificationQueryUseCase {
    @Suppress("LongParameterList")
    override fun list(
        actorId: UUID,
        onlyUnread: Boolean,
        projectId: UUID?,
        type: NotificationType?,
        page: Int,
        size: Int,
    ): PagedResponse<NotificationResponse> {
        val result =
            notificationRepository.search(
                NotificationSearchCriteria(
                    recipientId = actorId,
                    onlyUnread = onlyUnread,
                    projectId = projectId,
                    type = type,
                ),
                PageRequest(page, size),
            )

        return PagedResponse(
            items = result.content.map(NotificationResponse::from),
            pagination =
                PaginationMeta(
                    total = result.totalElements,
                    page = result.page,
                    pageSize = result.size,
                    totalPages = result.totalPages,
                    hasMore = result.page + 1 < result.totalPages,
                ),
        )
    }

    override fun unreadCount(actorId: UUID): Long = notificationRepository.countUnread(actorId)

    override fun getSettings(actorId: UUID): NotificationSettingsResponse =
        NotificationSettingsResponse.from(settingsRepository.findByUserId(actorId))
}
