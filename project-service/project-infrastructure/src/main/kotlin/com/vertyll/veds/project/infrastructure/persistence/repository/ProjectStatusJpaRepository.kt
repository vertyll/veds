package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectStatusJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ProjectStatusJpaRepository : JpaRepository<ProjectStatusJpaEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<ProjectStatusJpaEntity>
}
