package com.vertyll.veds.notification.infrastructure.persistence.adapter

import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import com.vertyll.veds.notification.infrastructure.IntegrationTestBase
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class NotificationSettingsPersistenceIntegrationTest(
    private val settings: NotificationSettingsRepository,
) : IntegrationTestBase() {
    @Test
    fun `someone who never saved settings has the defaults`() {
        val userId = UUID.randomUUID()

        val found = settings.findByUserId(userId)

        assertEquals(NotificationSettings.defaultFor(userId), found)
    }

    @Test
    fun `saved settings come back as they were written`() {
        val userId = UUID.randomUUID()
        val wanted =
            NotificationSettings(
                userId = userId,
                mutedTypes = setOf(NotificationType.TASK_CREATED),
                emailEnabledTypes = setOf(NotificationType.TASK_COMMENT_ADDED),
            )

        val saved = settings.save(wanted)
        val found = settings.findByUserId(userId)

        assertEquals(wanted.mutedTypes, found.mutedTypes)
        assertEquals(wanted.emailEnabledTypes, found.emailEnabledTypes)
        assertEquals(saved.version, found.version)
    }
}
