package com.vertyll.veds.project.infrastructure.web.security

import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.sharederror.ApiException
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val EMAIL_CLAIM = "email"
    private const val GIVEN_NAME_CLAIM = "given_name"
    private const val FAMILY_NAME_CLAIM = "family_name"
    private const val CLAIM_PARAM = "claim"
    private const val LANGUAGE_PARAM = "language"

    fun identityOf(jwt: Jwt): ActorIdentity {
        val subject = jwt.subject ?: throw claimMissing("sub")
        val id =
            try {
                UUID.fromString(subject)
            } catch (e: IllegalArgumentException) {
                throw ApiException(
                    ProjectError.TOKEN_CLAIM_MISSING,
                    mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
                )
            }

        return ActorIdentity(
            id = id,
            email = jwt.getClaimAsString(EMAIL_CLAIM) ?: throw claimMissing(EMAIL_CLAIM),
            firstName = jwt.getClaimAsString(GIVEN_NAME_CLAIM),
            lastName = jwt.getClaimAsString(FAMILY_NAME_CLAIM),
        )
    }

    fun idOf(jwt: Jwt): UUID = identityOf(jwt).id

    fun emailOf(jwt: Jwt): String = identityOf(jwt).email

    fun languageOf(acceptLanguage: String?): LanguageTag {
        if (acceptLanguage.isNullOrBlank()) {
            throw ApiException(ProjectError.LANGUAGE_NOT_SUPPLIED)
        }
        return LanguageTag.parse(acceptLanguage)
            ?: throw ApiException(
                ProjectError.LANGUAGE_NOT_SUPPORTED,
                mapOf(LANGUAGE_PARAM to acceptLanguage),
            )
    }

    private fun claimMissing(claim: String) = ApiException(ProjectError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to claim))
}
