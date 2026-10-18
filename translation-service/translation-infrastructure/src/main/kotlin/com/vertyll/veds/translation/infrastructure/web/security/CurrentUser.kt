package com.vertyll.veds.translation.infrastructure.web.security

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.translation.domain.error.TranslationError
import com.vertyll.veds.translation.domain.model.LanguageTag
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal object CurrentUser {
    private const val CLAIM_PARAM = "claim"

    fun idOf(jwt: Jwt): UUID {
        val subject =
            jwt.subject
                ?: throw ApiException(TranslationError.TOKEN_CLAIM_MISSING, mapOf(CLAIM_PARAM to "sub"))

        return try {
            UUID.fromString(subject)
        } catch (e: IllegalArgumentException) {
            throw ApiException(
                TranslationError.TOKEN_CLAIM_MISSING,
                mapOf(CLAIM_PARAM to "sub", "reason" to (e.message ?: "not a UUID")),
            )
        }
    }

    fun languageOf(language: String): LanguageTag {
        if (language.isBlank()) {
            throw ApiException(TranslationError.LANGUAGE_NOT_SUPPLIED)
        }
        return LanguageTag.parse(language)
            ?: throw ApiException(
                TranslationError.LANGUAGE_NOT_SUPPORTED,
                mapOf("language" to language),
            )
    }
}
