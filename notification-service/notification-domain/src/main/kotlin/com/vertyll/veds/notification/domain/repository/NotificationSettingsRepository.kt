package com.vertyll.veds.notification.domain.repository

import com.vertyll.veds.notification.domain.model.NotificationSettings
import java.util.UUID

interface NotificationSettingsRepository {
    fun save(settings: NotificationSettings): NotificationSettings

    fun findByUserId(userId: UUID): NotificationSettings
}
