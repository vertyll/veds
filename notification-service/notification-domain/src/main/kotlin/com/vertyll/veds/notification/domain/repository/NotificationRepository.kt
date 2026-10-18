package com.vertyll.veds.notification.domain.repository

import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationSearchCriteria
import com.vertyll.veds.notification.domain.model.PageRequest
import com.vertyll.veds.notification.domain.model.PageResult
import java.util.UUID

interface NotificationRepository {
    fun save(notification: Notification): Notification

    fun saveAll(notifications: Collection<Notification>): List<Notification>

    fun findById(id: UUID): Notification?

    fun search(
        criteria: NotificationSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<Notification>

    fun countUnread(recipientId: UUID): Long

    fun findAllUnreadBy(recipientId: UUID): List<Notification>

    fun findAllActiveBy(recipientId: UUID): List<Notification>

    fun findAllBySubjectId(subjectId: UUID): List<Notification>
}
