package com.vertyll.veds.translation.application

import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.PageResult
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository

internal class InMemoryKeyRepository : TranslationKeyRepository {
    val stored = linkedMapOf<String, TranslationKey>()

    fun given(vararg keys: TranslationKey) = keys.forEach { stored[it.key] = it }

    override fun save(key: TranslationKey) = key.also { stored[it.key] = it }

    override fun saveAll(keys: Collection<TranslationKey>) = keys.map { save(it) }

    override fun findByKey(key: String) = stored[key]

    override fun search(
        searchTerm: String?,
        sourceService: String?,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun findAll() = stored.values.toList()
}
