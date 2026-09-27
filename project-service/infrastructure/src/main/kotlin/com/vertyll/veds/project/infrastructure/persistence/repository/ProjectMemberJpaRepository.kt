package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectMemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
internal interface ProjectMemberJpaRepository : JpaRepository<ProjectMemberJpaEntity, UUID> {
    fun findByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    ): Optional<ProjectMemberJpaEntity>

    fun findAllByProjectId(projectId: UUID): List<ProjectMemberJpaEntity>

    fun findAllByUserId(userId: UUID): List<ProjectMemberJpaEntity>

    @Query(
        """
        SELECT m.projectId AS projectId, COUNT(m) AS memberCount
        FROM ProjectMemberJpaEntity m
        WHERE m.projectId IN :projectIds
        GROUP BY m.projectId
        """,
    )
    fun countByProjectIds(
        @Param("projectIds") projectIds: Collection<UUID>,
    ): List<ProjectMemberCountProjection>
}
