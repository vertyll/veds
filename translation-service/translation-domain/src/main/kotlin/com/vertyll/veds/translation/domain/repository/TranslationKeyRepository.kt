package com.vertyll.veds.translation.domain.repository

import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.PageResult
import com.vertyll.veds.translation.domain.model.TranslationKey

interface TranslationKeyRepository {
    fun save(key: TranslationKey): TranslationKey

    fun saveAll(keys: Collection<TranslationKey>): List<TranslationKey>

    fun findByKey(key: String): TranslationKey?

    fun search(
        searchTerm: String?,
        sourceService: String?,
        pageRequest: PageRequest,
    ): PageResult<TranslationKey>

    fun findAll(): List<TranslationKey>
}
