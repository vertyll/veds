package com.vertyll.veds.file.infrastructure.web.security

import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.domain.error.FileError
import com.vertyll.veds.sharederror.ApiException
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val CLAIM_PARAM = "claim"
    private const val EMAIL_CLAIM = "email"

    fun actorOf(jwt: Jwt): Actor {
        val subject =
            jwt.subject ?: throw ApiException(FileError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to "sub"))

        val id =
            try {
                UUID.fromString(subject)
            } catch (e: IllegalArgumentException) {
                throw ApiException(
                    FileError.TOKEN_CLAIM_MISSING,
                    mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
                )
            }

        return Actor(
            id = id,
            email =
                jwt.getClaimAsString(EMAIL_CLAIM)
                    ?: throw ApiException(FileError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to EMAIL_CLAIM)),
        )
    }
}
