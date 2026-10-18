package com.vertyll.veds.notification.application.command

import java.util.UUID

data class DismissNotificationsCommand(
    val notificationIds: Set<UUID>,
)
