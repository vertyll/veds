package com.vertyll.veds.notification.application

import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationSearchCriteria
import com.vertyll.veds.notification.domain.model.PageRequest
import com.vertyll.veds.notification.domain.model.PageResult
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import java.util.UUID

internal class InMemoryNotificationRepository : NotificationRepository {
    val stored = linkedMapOf<UUID, Notification>()

    fun given(vararg notifications: Notification) = notifications.forEach { stored[it.id] = it }

    override fun save(notification: Notification) = notification.also { stored[it.id] = it }

    override fun saveAll(notifications: Collection<Notification>) = notifications.map { save(it) }

    override fun findById(id: UUID) = stored[id]

    override fun search(
        criteria: NotificationSearchCriteria,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun countUnread(recipientId: UUID) =
        stored.values.count { it.recipientId == recipientId && !it.isRead && it.isActive }.toLong()

    override fun findAllUnreadBy(recipientId: UUID) = stored.values.filter { it.recipientId == recipientId && !it.isRead }

    override fun findAllActiveBy(recipientId: UUID) = stored.values.filter { it.recipientId == recipientId && it.isActive }

    override fun findAllBySubjectId(subjectId: UUID) = stored.values.filter { it.subjectId == subjectId }
}
