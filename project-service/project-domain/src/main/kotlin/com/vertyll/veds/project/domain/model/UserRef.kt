package com.vertyll.veds.project.domain.model

import java.time.Instant
import java.util.UUID

data class UserRef(
    val userId: UUID,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarFileId: UUID? = null,
    val updatedAt: Instant = Instant.now(),
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { email }
}
