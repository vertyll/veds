package com.vertyll.veds.translation.infrastructure.persistence.adapter

import com.vertyll.veds.translation.domain.model.LanguageTag
import com.vertyll.veds.translation.domain.model.TranslationValue
import com.vertyll.veds.translation.domain.repository.TranslationValueRepository
import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationValueJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.repository.TranslationValueJpaRepository
import org.springframework.stereotype.Component

@Component
internal class TranslationValuePersistenceAdapter(
    private val repository: TranslationValueJpaRepository,
) : TranslationValueRepository {
    override fun save(value: TranslationValue): TranslationValue = repository.save(value.toEntity()).toDomain()

    override fun saveAll(values: Collection<TranslationValue>): List<TranslationValue> =
        repository.saveAll(values.map { it.toEntity() }).map { it.toDomain() }

    override fun find(
        key: String,
        language: LanguageTag,
    ): TranslationValue? = repository.findByKeyAndLanguage(key, language.value).orElse(null)?.toDomain()

    override fun findAllForKeys(keys: Collection<String>): List<TranslationValue> =
        if (keys.isEmpty()) emptyList() else repository.findAllByKeyIn(keys).map { it.toDomain() }

    override fun findAllForLanguage(language: LanguageTag): List<TranslationValue> =
        repository.findAllByLanguage(language.value).map { it.toDomain() }

    override fun latestChangeMarker(language: LanguageTag): String =
        repository.latestUpdate(language.value)?.toEpochMilli()?.toString() ?: "0"
}

private fun TranslationValue.toEntity() =
    TranslationValueJpaEntity(
        id = id,
        key = key,
        language = language.value,
        defaultValue = defaultValue,
        overrideValue = overrideValue,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )

private fun TranslationValueJpaEntity.toDomain() =
    TranslationValue(
        id = id,
        key = key,
        language = LanguageTag.of(language),
        defaultValue = defaultValue,
        overrideValue = overrideValue,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
    )
