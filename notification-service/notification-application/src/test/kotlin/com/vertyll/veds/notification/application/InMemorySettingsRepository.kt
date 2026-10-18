package com.vertyll.veds.notification.application

import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import java.util.UUID

internal class InMemorySettingsRepository : NotificationSettingsRepository {
    val stored = linkedMapOf<UUID, NotificationSettings>()

    fun given(vararg settings: NotificationSettings) = settings.forEach { stored[it.userId] = it }

    override fun save(settings: NotificationSettings) = settings.also { stored[it.userId] = it }

    override fun findByUserId(userId: UUID) = stored[userId] ?: NotificationSettings.defaultFor(userId)
}
