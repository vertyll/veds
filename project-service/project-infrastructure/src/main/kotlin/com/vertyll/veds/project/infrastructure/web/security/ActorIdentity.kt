package com.vertyll.veds.project.infrastructure.web.security

import com.vertyll.veds.project.application.dto.Actor
import java.util.UUID

internal data class ActorIdentity(
    val id: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
) {
    fun toActor(): Actor =
        Actor(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
        )
}
