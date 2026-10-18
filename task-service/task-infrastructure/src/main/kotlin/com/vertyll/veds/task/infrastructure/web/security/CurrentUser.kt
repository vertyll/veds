package com.vertyll.veds.task.infrastructure.web.security

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.LanguageTag
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val EMAIL_CLAIM = "email"
    private const val GIVEN_NAME_CLAIM = "given_name"
    private const val FAMILY_NAME_CLAIM = "family_name"
    private const val CLAIM_PARAM = "claim"

    fun actorOf(jwt: Jwt): Actor {
        val subject = jwt.subject ?: throw claimMissing("sub")

        val id =
            try {
                UUID.fromString(subject)
            } catch (e: IllegalArgumentException) {
                throw ApiException(
                    TaskError.TOKEN_CLAIM_MISSING,
                    mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
                )
            }

        return Actor(
            id = id,
            email = jwt.getClaimAsString(EMAIL_CLAIM) ?: throw claimMissing(EMAIL_CLAIM),
            firstName = jwt.getClaimAsString(GIVEN_NAME_CLAIM),
            lastName = jwt.getClaimAsString(FAMILY_NAME_CLAIM),
        )
    }

    fun idOf(jwt: Jwt): UUID = actorOf(jwt).id

    fun languageOf(acceptLanguage: String?): LanguageTag {
        if (acceptLanguage.isNullOrBlank()) {
            throw ApiException(TaskError.LANGUAGE_NOT_SUPPLIED)
        }
        return LanguageTag.parse(acceptLanguage)
            ?: throw ApiException(
                TaskError.LANGUAGE_NOT_SUPPORTED,
                mapOf("language" to acceptLanguage),
            )
    }

    private fun claimMissing(claim: String) = ApiException(TaskError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to claim))
}
