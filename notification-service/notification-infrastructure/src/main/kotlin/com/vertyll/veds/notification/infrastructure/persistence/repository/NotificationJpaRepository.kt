package com.vertyll.veds.notification.infrastructure.persistence.repository

import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.infrastructure.persistence.entity.NotificationJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface NotificationJpaRepository : JpaRepository<NotificationJpaEntity, UUID> {
    @Query(
        """
        SELECT n FROM NotificationJpaEntity n
        WHERE n.recipientId = :recipientId
        AND n.isActive = TRUE
        AND (:onlyUnread = FALSE OR n.isRead = FALSE)
        AND (:projectId IS NULL OR n.projectId = :projectId)
        AND (:type IS NULL OR n.type = :type)
        ORDER BY n.createdAt DESC
        """,
    )
    fun search(
        @Param("recipientId") recipientId: UUID,
        @Param("onlyUnread") onlyUnread: Boolean,
        @Param("projectId") projectId: UUID?,
        @Param("type") type: NotificationType?,
        pageable: Pageable,
    ): Page<NotificationJpaEntity>

    fun countByRecipientIdAndIsReadFalseAndIsActiveTrue(recipientId: UUID): Long

    fun findAllByRecipientIdAndIsReadFalseAndIsActiveTrue(recipientId: UUID): List<NotificationJpaEntity>

    fun findAllByRecipientIdAndIsActiveTrue(recipientId: UUID): List<NotificationJpaEntity>

    fun findAllBySubjectId(subjectId: UUID): List<NotificationJpaEntity>
}
