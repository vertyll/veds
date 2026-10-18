package com.vertyll.veds.notification.infrastructure.persistence.repository

import com.vertyll.veds.notification.infrastructure.persistence.entity.NotificationSettingsJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface NotificationSettingsJpaRepository : JpaRepository<NotificationSettingsJpaEntity, UUID>
