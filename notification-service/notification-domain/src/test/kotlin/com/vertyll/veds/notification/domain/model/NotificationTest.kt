@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.notification.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class NotificationTest {
    private val recipient = Uuid.generateV7().toJavaUuid()

    private fun notification() =
        Notification.create(
            recipientId = recipient,
            type = NotificationType.TASK_ASSIGNED,
            params = mapOf("actorId" to Uuid.generateV7().toString()),
        )

    @Test
    fun `starts unread`() {
        val notification = notification()

        assertFalse(notification.isRead)
        assertNull(notification.readAt)
    }

    @Test
    fun `marking an already-read notification does not move the timestamp`() {
        val first = notification().markRead(Instant.parse("2026-01-01T10:00:00Z"))
        val second = first.markRead(Instant.parse("2026-01-02T10:00:00Z"))

        assertSame(first, second)
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), second.readAt)
    }

    @Test
    fun `marking unread clears the timestamp`() {
        val unread = notification().markRead().markUnread()

        assertFalse(unread.isRead)
        assertNull(unread.readAt)
    }

    @Test
    fun `retiring keeps the record but withdraws it`() {
        val retired = notification().retire()

        assertFalse(retired.isActive)
    }

    @Test
    fun `the message key comes from the type`() {
        assertEquals("notification.task_assigned", notification().type.key)
    }

    @Test
    fun `recognises its recipient`() {
        assertTrue(notification().isFor(recipient))
        assertFalse(notification().isFor(Uuid.generateV7().toJavaUuid()))
    }
}
