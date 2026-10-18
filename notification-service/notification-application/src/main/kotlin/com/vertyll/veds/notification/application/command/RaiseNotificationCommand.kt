package com.vertyll.veds.notification.application.command

import com.vertyll.veds.notification.domain.model.NotificationType
import java.util.UUID

data class RaiseNotificationCommand(
    val recipientIds: Set<UUID>,
    val type: NotificationType,
    val params: Map<String, String> = emptyMap(),
    val projectId: UUID? = null,
    val subjectId: UUID? = null,
    val fallbackEmail: String? = null,
    val excludeUserId: UUID? = null,
    val originSagaId: String? = null,
)
