package com.vertyll.veds.notification.infrastructure.persistence.entity

import com.vertyll.veds.notification.domain.model.NotificationType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        Index(name = "idx_notification_subject", columnList = "subject_id"),
        Index(name = "idx_notification_unread", columnList = "recipient_id, is_read, is_active"),
    ],
)
internal class NotificationJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "recipient_id", nullable = false)
    var recipientId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    var type: NotificationType,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_param", joinColumns = [JoinColumn(name = "notification_id")])
    @MapKeyColumn(name = "param_key", length = 64)
    @Column(name = "param_value", nullable = false, length = 512)
    var params: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "project_id")
    var projectId: UUID? = null,
    @Column(name = "subject_id")
    var subjectId: UUID? = null,
    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
    @Column(name = "read_at")
    var readAt: Instant? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
