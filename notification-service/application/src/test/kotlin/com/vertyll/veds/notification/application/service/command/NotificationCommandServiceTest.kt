@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.notification.application.service.command

import com.vertyll.veds.notification.application.InMemoryNotificationRepository
import com.vertyll.veds.notification.application.InMemoryRecipientDirectory
import com.vertyll.veds.notification.application.InMemorySettingsRepository
import com.vertyll.veds.notification.application.RecordingMailRequests
import com.vertyll.veds.notification.application.RecordingPush
import com.vertyll.veds.notification.application.SilentLogger
import com.vertyll.veds.notification.application.command.DismissNotificationsCommand
import com.vertyll.veds.notification.application.command.MarkReadCommand
import com.vertyll.veds.notification.application.command.RaiseNotificationCommand
import com.vertyll.veds.notification.application.command.RetireNotificationsCommand
import com.vertyll.veds.notification.application.command.UpdateSettingsCommand
import com.vertyll.veds.notification.domain.error.NotificationError
import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.domain.model.RecipientRef
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class NotificationCommandServiceTest {
    private val notifications = InMemoryNotificationRepository()
    private val settings = InMemorySettingsRepository()
    private val recipients = InMemoryRecipientDirectory()
    private val push = RecordingPush()
    private val mail = RecordingMailRequests()

    private val service = NotificationCommandService(notifications, settings, recipients, push, mail, SilentLogger)

    private val alice = Uuid.generateV7().toJavaUuid()
    private val bob = Uuid.generateV7().toJavaUuid()

    private fun raise(
        recipientIds: Set<UUID>,
        type: NotificationType = NotificationType.TASK_ASSIGNED,
        excludeUserId: UUID? = null,
        fallbackEmail: String? = null,
        subjectId: UUID? = null,
        originSagaId: String? = null,
    ) = service.raise(
        RaiseNotificationCommand(
            recipientIds = recipientIds,
            type = type,
            excludeUserId = excludeUserId,
            fallbackEmail = fallbackEmail,
            subjectId = subjectId,
            originSagaId = originSagaId,
        ),
    )

    @Test
    fun `every recipient gets their own notification`() {
        assertEquals(2, raise(setOf(alice, bob)))

        assertEquals(
            setOf(alice, bob),
            notifications.stored.values
                .map { it.recipientId }
                .toSet(),
        )
    }

    @Test
    fun `a raised notification is pushed to the recipient with a fresh unread count`() {
        raise(setOf(alice))

        assertEquals(listOf("push($alice,TASK_ASSIGNED)"), push.pushed)
        assertEquals(listOf(alice to 1L), push.unreadCounts)
    }

    @Test
    fun `the person who caused the event is not notified`() {
        assertEquals(1, raise(setOf(alice, bob), excludeUserId = alice))

        assertEquals(listOf(bob), notifications.stored.values.map { it.recipientId })
    }

    @Test
    fun `an event whose only recipient caused it raises nothing`() {
        assertEquals(0, raise(setOf(alice), excludeUserId = alice))

        assertTrue(push.pushed.isEmpty())
        assertTrue(mail.requested.isEmpty())
    }

    @Test
    fun `a muted type raises nothing for that user`() {
        settings.given(NotificationSettings(userId = alice, mutedTypes = setOf(NotificationType.TASK_ASSIGNED)))

        assertEquals(0, raise(setOf(alice)))

        assertTrue(notifications.stored.isEmpty())
        assertTrue(push.pushed.isEmpty())
    }

    @Test
    fun `one user's mute does not silence the others`() {
        settings.given(NotificationSettings(userId = alice, mutedTypes = setOf(NotificationType.TASK_ASSIGNED)))

        assertEquals(1, raise(setOf(alice, bob)))

        assertEquals(listOf(bob), notifications.stored.values.map { it.recipientId })
    }

    @Test
    fun `a type enabled for e-mail is also mailed to the recipient's address`() {
        recipients.given(RecipientRef(userId = alice, email = "alice@example.com"))

        raise(setOf(alice), type = NotificationType.TASK_ASSIGNED)

        assertEquals(listOf("alice@example.com:TASK_ASSIGNED"), mail.requested)
    }

    @Test
    fun `a type not enabled for e-mail is raised in-app only`() {
        recipients.given(RecipientRef(userId = alice, email = "alice@example.com"))

        raise(setOf(alice), type = NotificationType.TASK_COMMENT_ADDED)

        assertEquals(1, notifications.stored.size)
        assertTrue(mail.requested.isEmpty())
    }

    @Test
    fun `muting a type silences its e-mail as well`() {
        recipients.given(RecipientRef(userId = alice, email = "alice@example.com"))
        settings.given(NotificationSettings(userId = alice, mutedTypes = setOf(NotificationType.TASK_ASSIGNED)))

        raise(setOf(alice))

        assertTrue(mail.requested.isEmpty())
    }

    @Test
    fun `a recipient the directory does not know is mailed at the address supplied with the event`() {
        raise(setOf(alice), fallbackEmail = "invitee@example.com")

        assertEquals(listOf("invitee@example.com:TASK_ASSIGNED"), mail.requested)
    }

    @Test
    fun `a recipient with no known address still gets the in-app notification`() {
        assertEquals(1, raise(setOf(alice)))

        assertEquals(1, notifications.stored.size)
        assertTrue(mail.requested.isEmpty())
    }

    @Test
    fun `the directory address wins over the one supplied with the event`() {
        recipients.given(RecipientRef(userId = alice, email = "current@example.com"))

        raise(setOf(alice), fallbackEmail = "stale@example.com")

        assertEquals(listOf("current@example.com:TASK_ASSIGNED"), mail.requested)
    }

    private fun givenNotification(
        recipientId: UUID = alice,
        isRead: Boolean = false,
        subjectId: UUID? = null,
    ) = Notification(recipientId = recipientId, type = NotificationType.TASK_ASSIGNED, isRead = isRead, subjectId = subjectId)
        .also { notifications.given(it) }

    @Test
    fun `marking notifications read updates them and refreshes the badge`() {
        val first = givenNotification()
        val second = givenNotification()

        assertEquals(2, service.markRead(MarkReadCommand(setOf(first.id, second.id)), alice))

        assertTrue(notifications.findById(first.id)!!.isRead)
        assertEquals(listOf(alice to 0L), push.unreadCounts)
    }

    @Test
    fun `marking an already read notification read changes nothing`() {
        val read = givenNotification(isRead = true)

        assertEquals(0, service.markRead(MarkReadCommand(setOf(read.id)), alice))

        assertTrue(push.unreadCounts.isEmpty())
    }

    @Test
    fun `somebody else's notification cannot be marked read and looks missing`() {
        val theirs = givenNotification(recipientId = bob)

        val error = assertFailsWith<ApiException> { service.markRead(MarkReadCommand(setOf(theirs.id)), alice) }

        assertEquals(NotificationError.NOTIFICATION_NOT_FOUND, error.error)
        assertTrue(!notifications.findById(theirs.id)!!.isRead)
    }

    @Test
    fun `a batch containing somebody else's notification marks none of them read`() {
        val mine = givenNotification()
        val theirs = givenNotification(recipientId = bob)

        assertFailsWith<ApiException> { service.markRead(MarkReadCommand(setOf(mine.id, theirs.id)), alice) }

        assertTrue(!notifications.findById(mine.id)!!.isRead)
    }

    @Test
    fun `an unknown notification is reported as missing`() {
        val error = assertFailsWith<ApiException> { service.markRead(MarkReadCommand(setOf(Uuid.generateV7().toJavaUuid())), alice) }

        assertEquals(NotificationError.NOTIFICATION_NOT_FOUND, error.error)
    }

    @Test
    fun `marking everything read clears the caller's own unread notifications only`() {
        givenNotification()
        givenNotification()
        val theirs = givenNotification(recipientId = bob)

        assertEquals(2, service.markAllRead(alice))

        assertTrue(!notifications.findById(theirs.id)!!.isRead)
        assertEquals(listOf(alice to 0L), push.unreadCounts)
    }

    @Test
    fun `marking everything read with nothing unread does nothing`() {
        assertEquals(0, service.markAllRead(alice))

        assertTrue(push.unreadCounts.isEmpty())
    }

    @Test
    fun `dismissing a notification takes it off the list and refreshes the badge`() {
        val mine = givenNotification()

        assertEquals(1, service.dismiss(DismissNotificationsCommand(setOf(mine.id)), alice))

        assertTrue(!notifications.findById(mine.id)!!.isActive)
        assertEquals(listOf(alice to 0L), push.unreadCounts)
    }

    @Test
    fun `dismissing an already dismissed notification changes nothing`() {
        val mine = givenNotification()
        service.dismiss(DismissNotificationsCommand(setOf(mine.id)), alice)
        push.unreadCounts.clear()

        assertEquals(0, service.dismiss(DismissNotificationsCommand(setOf(mine.id)), alice))

        assertTrue(push.unreadCounts.isEmpty())
    }

    @Test
    fun `somebody else's notification cannot be dismissed and looks missing`() {
        val theirs = givenNotification(recipientId = bob)

        val error = assertFailsWith<ApiException> { service.dismiss(DismissNotificationsCommand(setOf(theirs.id)), alice) }

        assertEquals(NotificationError.NOTIFICATION_NOT_FOUND, error.error)
        assertTrue(notifications.findById(theirs.id)!!.isActive)
    }

    @Test
    fun `dismissing everything clears the caller's own notifications only`() {
        givenNotification()
        givenNotification(isRead = true)
        val theirs = givenNotification(recipientId = bob)

        assertEquals(2, service.dismissAll(alice))

        assertTrue(notifications.findById(theirs.id)!!.isActive)
        assertEquals(listOf(alice to 0L), push.unreadCounts)
    }

    @Test
    fun `dismissing everything with nothing left does nothing`() {
        assertEquals(0, service.dismissAll(alice))

        assertTrue(push.unreadCounts.isEmpty())
    }

    @Test
    fun `notifications about a vanished subject are retired`() {
        val subject = Uuid.generateV7().toJavaUuid()
        val about = givenNotification(subjectId = subject)
        val unrelated = givenNotification()

        assertEquals(1, service.retire(RetireNotificationsCommand(subject)))

        assertTrue(!notifications.findById(about.id)!!.isActive)
        assertTrue(notifications.findById(unrelated.id)!!.isActive)
    }

    @Test
    fun `retiring refreshes the badge of everyone affected`() {
        val subject = Uuid.generateV7().toJavaUuid()
        givenNotification(recipientId = alice, subjectId = subject)
        givenNotification(recipientId = bob, subjectId = subject)

        service.retire(RetireNotificationsCommand(subject))

        assertEquals(setOf(alice, bob), push.unreadCounts.map { it.first }.toSet())
        assertTrue(push.unreadCounts.all { it.second == 0L })
    }

    @Test
    fun `retiring a subject twice is harmless`() {
        val subject = Uuid.generateV7().toJavaUuid()
        givenNotification(subjectId = subject)

        assertEquals(1, service.retire(RetireNotificationsCommand(subject)))
        assertEquals(0, service.retire(RetireNotificationsCommand(subject)))
    }

    @Test
    fun `a subject nothing points at retires nothing`() {
        assertEquals(0, service.retire(RetireNotificationsCommand(Uuid.generateV7().toJavaUuid())))
    }

    @Test
    fun `an invitee with no account is e-mailed at the fallback address`() {
        assertEquals(0, raise(emptySet(), NotificationType.PROJECT_INVITATION, fallbackEmail = "invitee@example.com"))

        assertEquals(listOf("invitee@example.com:PROJECT_INVITATION"), mail.requested)
    }

    @Test
    fun `the originating saga travels with the e-mail request`() {
        raise(emptySet(), NotificationType.PROJECT_INVITATION, fallbackEmail = "i@example.com", originSagaId = "saga-7")

        assertEquals(listOf<String?>("saga-7"), mail.sagaIds)
    }

    @Test
    fun `no recipients and no fallback address sends nothing`() {
        assertEquals(0, raise(emptySet()))

        assertEquals(emptyList(), mail.requested)
    }

    @Test
    fun `settings replace what the user had before`() {
        val response =
            service.updateSettings(
                UpdateSettingsCommand(
                    mutedTypes = setOf(NotificationType.TASK_COMMENT_ADDED),
                    emailEnabledTypes = setOf(NotificationType.PROJECT_INVITATION),
                ),
                alice,
                version = null,
            )

        assertEquals(setOf(NotificationType.TASK_COMMENT_ADDED), settings.findByUserId(alice).mutedTypes)
        assertEquals(setOf(NotificationType.TASK_COMMENT_ADDED), response.mutedTypes)
    }

    @Test
    fun `an update against a stale version is refused`() {
        settings.given(NotificationSettings(userId = alice, version = 3L))

        val error =
            assertFailsWith<ApiException> {
                service.updateSettings(UpdateSettingsCommand(emptySet(), emptySet()), alice, version = 1L)
            }

        assertEquals(NotificationError.VERSION_MISMATCH, error.error)
    }

    @Test
    fun `a newly muted type is silent straight away`() {
        service.updateSettings(
            UpdateSettingsCommand(mutedTypes = setOf(NotificationType.TASK_ASSIGNED), emailEnabledTypes = emptySet()),
            alice,
            version = null,
        )

        assertEquals(0, raise(setOf(alice)))
    }
}
