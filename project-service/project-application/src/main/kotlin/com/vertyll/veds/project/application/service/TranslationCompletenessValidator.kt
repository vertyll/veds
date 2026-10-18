package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.port.outbound.SupportedLanguagesPort
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.sharederror.ApiException

class TranslationCompletenessValidator(
    private val supportedLanguages: SupportedLanguagesPort,
) {
    fun validate(translations: Set<Translation>) {
        val supported = supportedLanguages.supported()
        val provided = translations.map { it.language }.toSet()

        val missing = supported - provided
        if (missing.isNotEmpty()) {
            throw ApiException(
                ProjectError.TRANSLATION_MISSING,
                mapOf("missing" to missing.map { it.value }.sorted()),
            )
        }

        val unknown = provided - supported
        if (unknown.isNotEmpty()) {
            throw ApiException(
                ProjectError.LANGUAGE_NOT_SUPPORTED,
                mapOf("unsupported" to unknown.map { it.value }.sorted()),
            )
        }
    }
}
