package com.vertyll.veds.translation.application.service.query

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.translation.application.dto.LanguageResponse
import com.vertyll.veds.translation.application.dto.PagedResponse
import com.vertyll.veds.translation.application.dto.PaginationMeta
import com.vertyll.veds.translation.application.dto.TranslationKeyDetailsResponse
import com.vertyll.veds.translation.application.dto.TranslationSnapshotResponse
import com.vertyll.veds.translation.application.mapper.TranslationValueMapper
import com.vertyll.veds.translation.application.port.inbound.query.TranslationQueryUseCase
import com.vertyll.veds.translation.domain.error.TranslationError
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.model.TranslationValue
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository

class TranslationQueryService(
    private val keyRepository: TranslationKeyRepository,
    private val valueRepository: TranslationValueRepository,
    private val languageRepository: LanguageRepository,
) : TranslationQueryUseCase {
    override fun snapshot(language: String): TranslationSnapshotResponse {
        val tag = requireKnownLanguage(language)

        val entries =
            valueRepository
                .findAllForLanguage(tag)
                .mapNotNull { value -> value.effectiveValue?.let { value.key to it } }
                .toMap()

        return TranslationSnapshotResponse(
            language = tag.value,
            version = valueRepository.latestChangeMarker(tag),
            entries = entries,
        )
    }

    override fun languages(): List<LanguageResponse> =
        languageRepository.findAll().map {
            LanguageResponse(tag = it.tag.value, displayName = it.displayName, isDefault = it.isDefault)
        }

    override fun searchKeys(
        searchTerm: String?,
        sourceService: String?,
        onlyMissing: Boolean,
        page: Int,
        size: Int,
    ): PagedResponse<TranslationKeyDetailsResponse> {
        val allLanguages = languageRepository.findAll().map { it.tag }
        val result = keyRepository.search(searchTerm?.takeIf { it.isNotBlank() }, sourceService, PageRequest(page, size))
        val valuesByKey = valueRepository.findAllForKeys(result.content.map { it.key }).groupBy { it.key }

        val items =
            result.content
                .map { key -> details(key, valuesByKey[key.key].orEmpty(), allLanguages) }
                .filter { !onlyMissing || it.missingLanguages.isNotEmpty() }

        return PagedResponse(
            items = items,
            pagination =
                PaginationMeta(
                    total = result.totalElements,
                    page = result.page,
                    pageSize = result.size,
                    totalPages = result.totalPages,
                    hasMore = result.page + 1 < result.totalPages,
                ),
        )
    }

    override fun keyDetails(key: String): TranslationKeyDetailsResponse {
        val translationKey =
            keyRepository.findByKey(key)
                ?: throw ApiException(TranslationError.KEY_NOT_FOUND, mapOf("key" to key))

        return details(
            translationKey,
            valueRepository.findAllForKeys(listOf(key)),
            languageRepository.findAll().map { it.tag },
        )
    }

    private fun details(
        key: TranslationKey,
        values: List<TranslationValue>,
        allLanguages: List<LanguageTag>,
    ): TranslationKeyDetailsResponse {
        val covered = values.filter { it.effectiveValue != null }.map { it.language }.toSet()

        return TranslationKeyDetailsResponse(
            key = key.key,
            sourceService = key.sourceService,
            description = key.description,
            values = values.sortedBy { it.language.value }.map(TranslationValueMapper::toResponse),
            missingLanguages = (allLanguages - covered).map { it.value }.sorted(),
            createdAt = key.createdAt,
            updatedAt = key.updatedAt,
        )
    }

    private fun requireKnownLanguage(raw: String): LanguageTag {
        val tag =
            LanguageTag.parse(raw)
                ?: throw ApiException(TranslationError.LANGUAGE_NOT_FOUND, mapOf("language" to raw))
        languageRepository.findByTag(tag)
            ?: throw ApiException(TranslationError.LANGUAGE_NOT_FOUND, mapOf("language" to raw))
        return tag
    }
}
