package com.vertyll.veds.notification.application.port.inbound.command

import com.vertyll.veds.notification.application.command.DismissNotificationsCommand
import com.vertyll.veds.notification.application.command.MarkReadCommand
import com.vertyll.veds.notification.application.command.RaiseNotificationCommand
import com.vertyll.veds.notification.application.command.RetireNotificationsCommand
import com.vertyll.veds.notification.application.command.UpdateSettingsCommand
import com.vertyll.veds.notification.application.dto.NotificationSettingsResponse
import java.util.UUID

interface NotificationCommandUseCase {
    fun raise(command: RaiseNotificationCommand): Int

    fun markRead(
        command: MarkReadCommand,
        actorId: UUID,
    ): Int

    fun markAllRead(actorId: UUID): Int

    fun dismiss(
        command: DismissNotificationsCommand,
        actorId: UUID,
    ): Int

    fun dismissAll(actorId: UUID): Int

    fun updateSettings(
        command: UpdateSettingsCommand,
        actorId: UUID,
        version: Long? = null,
    ): NotificationSettingsResponse

    fun retire(command: RetireNotificationsCommand): Int
}
