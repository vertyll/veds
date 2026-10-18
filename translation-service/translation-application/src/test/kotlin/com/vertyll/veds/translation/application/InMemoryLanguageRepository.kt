package com.vertyll.veds.translation.application

import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.repository.LanguageRepository

internal class InMemoryLanguageRepository : LanguageRepository {
    val stored = linkedMapOf<LanguageTag, Language>()

    fun given(vararg languages: Language) = languages.forEach { stored[it.tag] = it }

    override fun save(language: Language) = language.also { stored[it.tag] = it }

    override fun findByTag(tag: LanguageTag) = stored[tag]

    override fun findAll() = stored.values.toList()

    override fun findDefault() = stored.values.firstOrNull { it.isDefault }
}
