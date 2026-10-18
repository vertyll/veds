package com.vertyll.veds.translation.application

import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.TranslationValue
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository

internal class InMemoryValueRepository : TranslationValueRepository {
    val stored = linkedMapOf<Pair<String, LanguageTag>, TranslationValue>()

    fun given(vararg values: TranslationValue) = values.forEach { stored[it.key to it.language] = it }

    override fun save(value: TranslationValue) = value.also { stored[it.key to it.language] = it }

    override fun saveAll(values: Collection<TranslationValue>) = values.map { save(it) }

    override fun find(
        key: String,
        language: LanguageTag,
    ) = stored[key to language]

    override fun findAllForKeys(keys: Collection<String>) = stored.values.filter { it.key in keys }

    override fun findAllForLanguage(language: LanguageTag) = stored.values.filter { it.language == language }

    override fun latestChangeMarker(language: LanguageTag) =
        findAllForLanguage(language).maxOfOrNull { it.updatedAt.toEpochMilli() }?.toString().orEmpty()
}
