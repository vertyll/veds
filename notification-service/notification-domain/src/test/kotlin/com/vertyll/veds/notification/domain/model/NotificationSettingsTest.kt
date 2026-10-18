@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.notification.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class NotificationSettingsTest {
    private val userId = Uuid.generateV7().toJavaUuid()

    @Test
    fun `defaults notify in-app for everything`() {
        val settings = NotificationSettings.defaultFor(userId)

        NotificationType.entries.forEach { type ->
            assertTrue(settings.allows(type, NotificationChannel.IN_APP), "$type should be allowed in-app")
        }
    }

    @Test
    fun `defaults e-mail only for invitations and assignments`() {
        val settings = NotificationSettings.defaultFor(userId)

        assertTrue(settings.allows(NotificationType.PROJECT_INVITATION, NotificationChannel.EMAIL))
        assertTrue(settings.allows(NotificationType.TASK_ASSIGNED, NotificationChannel.EMAIL))
        assertFalse(settings.allows(NotificationType.TASK_COMMENT_ADDED, NotificationChannel.EMAIL))
    }

    @Test
    fun `muting a type silences it on every channel`() {
        val settings =
            NotificationSettings
                .defaultFor(userId)
                .mute(NotificationType.TASK_ASSIGNED)

        assertFalse(settings.allows(NotificationType.TASK_ASSIGNED, NotificationChannel.IN_APP))
        assertFalse(settings.allows(NotificationType.TASK_ASSIGNED, NotificationChannel.EMAIL))
    }

    @Test
    fun `unmuting restores the previous behaviour`() {
        val settings =
            NotificationSettings
                .defaultFor(userId)
                .mute(NotificationType.TASK_ASSIGNED)
                .unmute(NotificationType.TASK_ASSIGNED)

        assertTrue(settings.allows(NotificationType.TASK_ASSIGNED, NotificationChannel.EMAIL))
    }

    @Test
    fun `enabling e-mail for a type that had none works`() {
        val settings = NotificationSettings.defaultFor(userId).enableEmail(NotificationType.TASK_STATUS_CHANGED)

        assertTrue(settings.allows(NotificationType.TASK_STATUS_CHANGED, NotificationChannel.EMAIL))
    }
}
