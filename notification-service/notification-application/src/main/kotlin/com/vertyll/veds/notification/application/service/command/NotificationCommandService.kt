package com.vertyll.veds.notification.application.service.command

import com.vertyll.veds.notification.application.command.DismissNotificationsCommand
import com.vertyll.veds.notification.application.command.MarkReadCommand
import com.vertyll.veds.notification.application.command.RaiseNotificationCommand
import com.vertyll.veds.notification.application.command.RetireNotificationsCommand
import com.vertyll.veds.notification.application.command.UpdateSettingsCommand
import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.dto.NotificationSettingsResponse
import com.vertyll.veds.notification.application.port.inbound.command.NotificationCommandUseCase
import com.vertyll.veds.notification.application.port.outbound.MailRequestPort
import com.vertyll.veds.notification.application.port.outbound.NotificationPushPort
import com.vertyll.veds.notification.application.port.outbound.UseCaseLogger
import com.vertyll.veds.notification.domain.error.NotificationError
import com.vertyll.veds.notification.domain.model.Notification
import com.vertyll.veds.notification.domain.model.NotificationChannel
import com.vertyll.veds.notification.domain.model.NotificationSettings
import com.vertyll.veds.notification.domain.model.VersionGuard
import com.vertyll.veds.notification.domain.repository.NotificationRepository
import com.vertyll.veds.notification.domain.repository.NotificationSettingsRepository
import com.vertyll.veds.notification.domain.repository.RecipientDirectoryRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

@Suppress("LongParameterList")
class NotificationCommandService(
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: NotificationSettingsRepository,
    private val recipients: RecipientDirectoryRepository,
    private val push: NotificationPushPort,
    private val mail: MailRequestPort,
    private val logger: UseCaseLogger,
) : NotificationCommandUseCase {
    override fun raise(command: RaiseNotificationCommand): Int {
        val targets = command.recipientIds - setOfNotNull(command.excludeUserId)
        if (targets.isEmpty()) {
            requestMailForRecipientWithoutAccount(command)
            return 0
        }

        var raised = 0

        targets.forEach { recipientId ->
            val settings = settingsRepository.findByUserId(recipientId)

            if (!settings.allows(command.type, NotificationChannel.IN_APP)) {
                logger.debug("{} muted {} - nothing raised", recipientId, command.type)
                return@forEach
            }

            val notification =
                notificationRepository.save(
                    Notification.create(
                        recipientId = recipientId,
                        type = command.type,
                        params = command.params,
                        projectId = command.projectId,
                        subjectId = command.subjectId,
                    ),
                )
            raised++

            push.push(recipientId, NotificationResponse.from(notification))
            push.pushUnreadCount(recipientId, notificationRepository.countUnread(recipientId))

            requestMailIfAllowed(command, settings, recipientId)
        }

        return raised
    }

    private fun requestMailIfAllowed(
        command: RaiseNotificationCommand,
        settings: NotificationSettings,
        recipientId: UUID,
    ) {
        if (!settings.allows(command.type, NotificationChannel.EMAIL)) return

        val address = recipients.findById(recipientId)?.email ?: command.fallbackEmail
        if (address == null) {
            logger.warn("No address known for {} - skipping e-mail for {}", recipientId, command.type)
            return
        }

        mail.requestMail(
            to = address,
            type = command.type,
            params = command.params,
            originSagaId = command.originSagaId,
        )
    }

    private fun requestMailForRecipientWithoutAccount(command: RaiseNotificationCommand) {
        val address = command.fallbackEmail ?: return
        mail.requestMail(
            to = address,
            type = command.type,
            params = command.params,
            originSagaId = command.originSagaId,
        )
    }

    override fun markRead(
        command: MarkReadCommand,
        actorId: UUID,
    ): Int {
        val notifications = command.notificationIds.map { ownedBy(actorId, it) }

        val changed = notifications.filterNot { it.isRead }
        if (changed.isEmpty()) return 0

        notificationRepository.saveAll(changed.map { it.markRead() })
        push.pushUnreadCount(actorId, notificationRepository.countUnread(actorId))
        return changed.size
    }

    private fun ownedBy(
        actorId: UUID,
        notificationId: UUID,
    ): Notification {
        val notification = notificationRepository.findById(notificationId)
        if (notification == null || !notification.isFor(actorId)) {
            throw ApiException(
                NotificationError.NOTIFICATION_NOT_FOUND,
                mapOf("notificationId" to notificationId.toString()),
            )
        }
        return notification
    }

    override fun markAllRead(actorId: UUID): Int {
        val unread = notificationRepository.findAllUnreadBy(actorId)
        if (unread.isEmpty()) return 0

        notificationRepository.saveAll(unread.map { it.markRead() })
        push.pushUnreadCount(actorId, 0)
        return unread.size
    }

    override fun dismiss(
        command: DismissNotificationsCommand,
        actorId: UUID,
    ): Int {
        val owned = command.notificationIds.map { ownedBy(actorId, it) }
        val active = owned.filter { it.isActive }
        if (active.isEmpty()) return 0

        notificationRepository.saveAll(active.map { it.retire() })
        push.pushUnreadCount(actorId, notificationRepository.countUnread(actorId))
        return active.size
    }

    override fun dismissAll(actorId: UUID): Int {
        val active = notificationRepository.findAllActiveBy(actorId)
        if (active.isEmpty()) return 0

        notificationRepository.saveAll(active.map { it.retire() })
        push.pushUnreadCount(actorId, 0)
        return active.size
    }

    override fun updateSettings(
        command: UpdateSettingsCommand,
        actorId: UUID,
        version: Long?,
    ): NotificationSettingsResponse {
        val current = settingsRepository.findByUserId(actorId)

        VersionGuard.requireMatch(current.version, version) {
            ApiException(NotificationError.VERSION_MISMATCH)
        }

        val updated =
            settingsRepository.save(
                current.copy(
                    mutedTypes = command.mutedTypes,
                    emailEnabledTypes = command.emailEnabledTypes,
                ),
            )
        return NotificationSettingsResponse.from(updated)
    }

    override fun retire(command: RetireNotificationsCommand): Int {
        val affected = notificationRepository.findAllBySubjectId(command.subjectId).filter { it.isActive }
        if (affected.isEmpty()) return 0

        notificationRepository.saveAll(affected.map { it.retire() })
        affected.map { it.recipientId }.distinct().forEach { recipientId ->
            push.pushUnreadCount(recipientId, notificationRepository.countUnread(recipientId))
        }
        logger.info("Retired {} notifications for subject {}", affected.size, command.subjectId)
        return affected.size
    }
}
