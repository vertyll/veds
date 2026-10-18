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
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(name = "notification_settings")
internal class NotificationSettingsJpaEntity(
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_muted_type", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    var mutedTypes: MutableSet<NotificationType> = mutableSetOf(),
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_email_type", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    var emailEnabledTypes: MutableSet<NotificationType> = mutableSetOf(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
