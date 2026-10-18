package com.vertyll.veds.notification.domain.model

import java.time.Instant
import java.util.UUID

data class RecipientRef(
    val userId: UUID,
    val email: String,
    val displayName: String? = null,
    val updatedAt: Instant = Instant.now(),
)
