package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectRoleJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
internal interface ProjectRoleJpaRepository : JpaRepository<ProjectRoleJpaEntity, UUID> {
    fun findByCode(code: String): Optional<ProjectRoleJpaEntity>

    fun existsByCode(code: String): Boolean
}
