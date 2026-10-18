package com.vertyll.veds.translation.domain.repository

import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.TranslationValue

interface TranslationValueRepository {
    fun save(value: TranslationValue): TranslationValue

    fun saveAll(values: Collection<TranslationValue>): List<TranslationValue>

    fun find(
        key: String,
        language: LanguageTag,
    ): TranslationValue?

    fun findAllForKeys(keys: Collection<String>): List<TranslationValue>

    fun findAllForLanguage(language: LanguageTag): List<TranslationValue>

    fun latestChangeMarker(language: LanguageTag): String
}
