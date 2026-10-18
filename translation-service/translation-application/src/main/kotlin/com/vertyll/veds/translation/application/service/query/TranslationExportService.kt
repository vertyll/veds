package com.vertyll.veds.translation.application.service.query

import com.vertyll.veds.sharedtranslation.MessageResolver
import com.vertyll.veds.sharedtranslation.TranslationSource
import com.vertyll.veds.translation.application.dto.ExportRowResponse
import com.vertyll.veds.translation.application.port.inbound.query.TranslationExportUseCase
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository

class TranslationExportService(
    private val keyRepository: TranslationKeyRepository,
    private val valueRepository: TranslationValueRepository,
    private val languageRepository: LanguageRepository,
) : TranslationExportUseCase {
    private companion object {
        private val HEADER_KEYS =
            listOf(
                "translation.export.column.key",
                "translation.export.column.source_service",
                "translation.export.column.description",
            )
    }

    override fun exportRows(): List<ExportRowResponse> {
        val keys = keyRepository.findAll().sortedBy { it.key }
        val valuesByKey = valueRepository.findAllForKeys(keys.map { it.key }).groupBy { it.key }

        return keys.map { key ->
            val values = valuesByKey[key.key].orEmpty()
            ExportRowResponse(
                key = key.key,
                sourceService = key.sourceService,
                description = key.description,
                values = values.mapNotNull { v -> v.effectiveValue?.let { v.language.value to it } }.toMap(),
                defaultValues = values.mapNotNull { v -> v.defaultValue?.let { v.language.value to it } }.toMap(),
            )
        }
    }

    override fun exportHeaders(language: LanguageTag): List<String> {
        val resolver = MessageResolver(snapshotSource(language))

        val fixed = HEADER_KEYS.map { resolver.resolve(it, language.value) }
        val languageColumns = languageRepository.findAll().map { it.tag.value }
        return fixed + languageColumns
    }

    private fun snapshotSource(tag: LanguageTag): TranslationSource {
        val entries =
            valueRepository
                .findAllForLanguage(tag)
                .mapNotNull { value -> value.effectiveValue?.let { value.key to it } }
                .toMap()
        return TranslationSource { key, _ -> entries[key] }
    }
}
