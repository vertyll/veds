package com.vertyll.veds.notification.infrastructure.web.security

import com.vertyll.veds.notification.domain.error.NotificationError
import com.vertyll.veds.sharederror.ApiException
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val CLAIM_PARAM = "claim"

    fun idOf(jwt: Jwt): UUID {
        val subject =
            jwt.subject
                ?: throw ApiException(NotificationError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to "sub"))

        return try {
            UUID.fromString(subject)
        } catch (e: IllegalArgumentException) {
            throw ApiException(
                NotificationError.TOKEN_CLAIM_MISSING,
                mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
            )
        }
    }
}
