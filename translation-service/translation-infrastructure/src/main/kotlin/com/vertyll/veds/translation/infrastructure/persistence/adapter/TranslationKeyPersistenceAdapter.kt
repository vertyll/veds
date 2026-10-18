package com.vertyll.veds.translation.infrastructure.persistence.adapter

import com.vertyll.veds.translation.domain.model.PageRequest
import com.vertyll.veds.translation.domain.model.PageResult
import com.vertyll.veds.translation.domain.model.TranslationKey
import com.vertyll.veds.translation.domain.repository.TranslationKeyRepository
import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationKeyJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.repository.TranslationKeyJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class TranslationKeyPersistenceAdapter(
    private val repository: TranslationKeyJpaRepository,
) : TranslationKeyRepository {
    override fun save(key: TranslationKey): TranslationKey = repository.save(key.toEntity()).toDomain()

    override fun saveAll(keys: Collection<TranslationKey>): List<TranslationKey> =
        repository.saveAll(keys.map { it.toEntity() }).map { it.toDomain() }

    override fun findByKey(key: String): TranslationKey? = repository.findByIdOrNull(key)?.toDomain()

    override fun search(
        searchTerm: String?,
        sourceService: String?,
        pageRequest: PageRequest,
    ): PageResult<TranslationKey> {
        val page =
            repository.search(
                searchTerm = searchTerm,
                sourceService = sourceService,
                pageable =
                    org.springframework.data.domain.PageRequest
                        .of(pageRequest.page, pageRequest.size),
            )
        return PageResult(
            content = page.content.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = page.totalElements,
        )
    }

    override fun findAll(): List<TranslationKey> = repository.findAll().map { it.toDomain() }
}

private fun TranslationKey.toEntity() =
    TranslationKeyJpaEntity(
        key = key,
        sourceService = sourceService,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun TranslationKeyJpaEntity.toDomain() =
    TranslationKey(
        key = key,
        sourceService = sourceService,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
