package com.vertyll.veds.notification.application.command

import java.util.UUID

data class MarkReadCommand(
    val notificationIds: Set<UUID>,
)
