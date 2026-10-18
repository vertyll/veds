package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.UserRef
import java.util.UUID

data class Actor(
    val id: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
) {
    fun toUserRef(): UserRef =
        UserRef(
            userId = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
        )
}
