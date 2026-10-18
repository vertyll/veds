package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.project.application.port.outbound.SupportedLanguagesPort
import com.vertyll.veds.project.domain.model.LanguageTag
import org.springframework.stereotype.Component

@Component
internal class SupportedLanguagesAdapter(
    properties: TranslationLanguagesProperties,
) : SupportedLanguagesPort {
    private val languages: Set<LanguageTag> =
        properties.supportedLanguages
            .map { LanguageTag.of(it) }
            .toSet()
            .also { check(it.isNotEmpty()) { "application.translation.supported-languages must not be empty" } }

    override fun supported(): Set<LanguageTag> = languages
}
