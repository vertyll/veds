package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.ProjectMember
import java.util.UUID

interface ProjectMemberRepository {
    fun save(member: ProjectMember): ProjectMember

    fun findById(id: UUID): ProjectMember?

    fun findByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    ): ProjectMember?

    fun findAllByProjectId(projectId: UUID): List<ProjectMember>

    fun findAllByUserId(userId: UUID): List<ProjectMember>

    fun countByProjectIds(projectIds: Collection<UUID>): Map<UUID, Int>

    fun delete(id: UUID)
}
