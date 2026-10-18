package com.vertyll.veds.translation.infrastructure.persistence.repository

import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationKeyJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
internal interface TranslationKeyJpaRepository : JpaRepository<TranslationKeyJpaEntity, String> {
    @Query(
        """
        SELECT k FROM TranslationKeyJpaEntity k
        WHERE (:sourceService IS NULL OR k.sourceService = :sourceService)
        AND (
            :searchTerm IS NULL
            OR LOWER(k.key) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
            OR LOWER(COALESCE(k.description, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
        )
        ORDER BY k.key
        """,
    )
    fun search(
        @Param("searchTerm") searchTerm: String?,
        @Param("sourceService") sourceService: String?,
        pageable: Pageable,
    ): Page<TranslationKeyJpaEntity>
}
