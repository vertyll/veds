package com.vertyll.veds.translation.infrastructure.persistence.repository

import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationValueJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
internal interface TranslationValueJpaRepository : JpaRepository<TranslationValueJpaEntity, UUID> {
    fun findByKeyAndLanguage(
        key: String,
        language: String,
    ): Optional<TranslationValueJpaEntity>

    fun findAllByKeyIn(keys: Collection<String>): List<TranslationValueJpaEntity>

    fun findAllByLanguage(language: String): List<TranslationValueJpaEntity>

    @Query("SELECT MAX(v.updatedAt) FROM TranslationValueJpaEntity v WHERE v.language = :language")
    fun latestUpdate(
        @Param("language") language: String,
    ): Instant?
}
