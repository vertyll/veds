package com.vertyll.veds.notification.infrastructure.persistence.adapter

import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationSearchCriteria
import com.vertyll.veds.notification.domain.model.PageRequest
import com.vertyll.veds.notification.domain.model.PageResult
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.infrastructure.persistence.entity.NotificationJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.repository.NotificationJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Component
internal class NotificationPersistenceAdapter(
    private val repository: NotificationJpaRepository,
) : NotificationRepository {
    override fun save(notification: Notification): Notification = repository.save(notification.toEntity()).toDomain()

    override fun saveAll(notifications: Collection<Notification>): List<Notification> =
        repository.saveAll(notifications.map { it.toEntity() }).map { it.toDomain() }

    override fun findById(id: UUID): Notification? = repository.findByIdOrNull(id)?.toDomain()

    override fun search(
        criteria: NotificationSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<Notification> {
        val page =
            repository.search(
                recipientId = criteria.recipientId,
                onlyUnread = criteria.onlyUnread,
                projectId = criteria.projectId,
                type = criteria.type,
                pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size),
            )
        return PageResult(
            content = page.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = page.totalElements,
        )
    }

    override fun countUnread(recipientId: UUID): Long = repository.countByRecipientIdAndIsReadFalseAndIsActiveTrue(recipientId)

    override fun findAllUnreadBy(recipientId: UUID): List<Notification> =
        repository.findAllByRecipientIdAndIsReadFalseAndIsActiveTrue(recipientId).map { it.toDomain() }

    override fun findAllActiveBy(recipientId: UUID): List<Notification> =
        repository.findAllByRecipientIdAndIsActiveTrue(recipientId).map { it.toDomain() }

    override fun findAllBySubjectId(subjectId: UUID): List<Notification> = repository.findAllBySubjectId(subjectId).map { it.toDomain() }
}

private fun Notification.toEntity() =
    NotificationJpaEntity(
        id = id,
        recipientId = recipientId,
        type = type,
        params = params.toMutableMap(),
        projectId = projectId,
        subjectId = subjectId,
        isRead = isRead,
        readAt = readAt,
        isActive = isActive,
        createdAt = createdAt,
        version = version,
    )

internal fun NotificationJpaEntity.toDomain() =
    Notification(
        id = id,
        recipientId = recipientId,
        type = type,
        params = params.toMap(),
        projectId = projectId,
        subjectId = subjectId,
        isRead = isRead,
        readAt = readAt,
        isActive = isActive,
        createdAt = createdAt,
        version = version,
    )
