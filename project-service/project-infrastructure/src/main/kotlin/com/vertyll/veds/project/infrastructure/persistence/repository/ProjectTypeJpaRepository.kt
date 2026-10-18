package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.domain.model.ProjectTypeCode
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectTypeJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
internal interface ProjectTypeJpaRepository : JpaRepository<ProjectTypeJpaEntity, UUID> {
    fun findByCode(code: ProjectTypeCode): Optional<ProjectTypeJpaEntity>

    fun existsByCode(code: ProjectTypeCode): Boolean
}
