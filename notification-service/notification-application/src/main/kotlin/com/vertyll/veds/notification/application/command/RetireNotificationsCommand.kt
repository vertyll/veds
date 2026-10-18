package com.vertyll.veds.notification.application.command

import java.util.UUID

data class RetireNotificationsCommand(
    val subjectId: UUID,
)
