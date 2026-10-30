package com.vertyll.veds.notification.infrastructure.persistence.adapter

import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import com.vertyll.veds.notification.infrastructure.persistence.entity.NotificationSettingsJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.repository.NotificationSettingsJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class NotificationSettingsPersistenceAdapter(
    private val repository: NotificationSettingsJpaRepository,
) : NotificationSettingsRepository {
    override fun save(settings: NotificationSettings): NotificationSettings =
        repository
            .save(
                NotificationSettingsJpaEntity(
                    userId = settings.userId,
                    mutedTypes = settings.mutedTypes.toMutableSet(),
                    emailEnabledTypes = settings.emailEnabledTypes.toMutableSet(),
                    version = settings.version,
                ),
            ).let { entity ->
                NotificationSettings(
                    userId = entity.userId,
                    mutedTypes = entity.mutedTypes.toSet(),
                    emailEnabledTypes = entity.emailEnabledTypes.toSet(),
                    version = entity.version,
                )
            }

    override fun findByUserId(userId: UUID): NotificationSettings =
        repository.findByIdOrNull(userId)?.let { entity ->
            NotificationSettings(
                userId = entity.userId,
                mutedTypes = entity.mutedTypes.toSet(),
                emailEnabledTypes = entity.emailEnabledTypes.toSet(),
                version = entity.version,
            )
        } ?: NotificationSettings.defaultFor(userId)
}
