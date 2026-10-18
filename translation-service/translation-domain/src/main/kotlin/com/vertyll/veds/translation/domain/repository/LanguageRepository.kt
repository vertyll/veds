package com.vertyll.veds.translation.domain.repository

import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag

interface LanguageRepository {
    fun save(language: Language): Language

    fun findByTag(tag: LanguageTag): Language?

    fun findAll(): List<Language>

    fun findDefault(): Language?
}
