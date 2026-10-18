package com.vertyll.veds.translation.infrastructure.persistence.adapter

import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.repository.LanguageRepository
import com.vertyll.veds.translation.infrastructure.persistence.entity.LanguageJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.repository.LanguageJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class LanguagePersistenceAdapter(
    private val repository: LanguageJpaRepository,
) : LanguageRepository {
    override fun save(language: Language): Language = repository.save(language.toEntity()).toDomain()

    override fun findByTag(tag: LanguageTag): Language? = repository.findByIdOrNull(tag.value)?.toDomain()

    override fun findAll(): List<Language> = repository.findAll().map { it.toDomain() }

    override fun findDefault(): Language? = repository.findByIsDefaultTrue().orElse(null)?.toDomain()
}

private fun Language.toEntity() =
    LanguageJpaEntity(
        tag = tag.value,
        displayName = displayName,
        isDefault = isDefault,
        createdAt = createdAt,
    )

private fun LanguageJpaEntity.toDomain() =
    Language(
        tag = LanguageTag.of(tag),
        displayName = displayName,
        isDefault = isDefault,
        createdAt = createdAt,
    )
