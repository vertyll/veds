package com.vertyll.veds.task.infrastructure.persistence.repository

import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectMembershipRefId
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectMembershipRefJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
internal interface ProjectMembershipRefJpaRepository : JpaRepository<ProjectMembershipRefJpaEntity, ProjectMembershipRefId> {
    fun findAllByProjectId(projectId: UUID): List<ProjectMembershipRefJpaEntity>

    fun deleteByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    )
}
