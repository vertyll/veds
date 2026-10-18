package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectStatusRefJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ProjectStatusRefJpaRepository : JpaRepository<ProjectStatusRefJpaEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<ProjectStatusRefJpaEntity>
}
