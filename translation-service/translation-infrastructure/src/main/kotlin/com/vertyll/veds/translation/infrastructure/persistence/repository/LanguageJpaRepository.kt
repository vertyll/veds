package com.vertyll.veds.translation.infrastructure.persistence.repository

import com.vertyll.veds.translation.infrastructure.persistence.entity.LanguageJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
internal interface LanguageJpaRepository : JpaRepository<LanguageJpaEntity, String> {
    fun findByIsDefaultTrue(): Optional<LanguageJpaEntity>
}
