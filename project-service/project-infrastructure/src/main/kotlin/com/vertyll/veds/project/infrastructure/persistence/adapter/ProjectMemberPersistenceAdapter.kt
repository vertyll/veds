package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectMemberJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectMemberJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ProjectMemberPersistenceAdapter(
    private val repository: ProjectMemberJpaRepository,
) : ProjectMemberRepository {
    override fun save(member: ProjectMember): ProjectMember = repository.save(member.toJpaEntity()).toDomain()

    override fun findById(id: UUID): ProjectMember? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    ): ProjectMember? = repository.findByProjectIdAndUserId(projectId, userId).orElse(null)?.toDomain()

    override fun findAllByProjectId(projectId: UUID): List<ProjectMember> = repository.findAllByProjectId(projectId).map { it.toDomain() }

    override fun findAllByUserId(userId: UUID): List<ProjectMember> = repository.findAllByUserId(userId).map { it.toDomain() }

    override fun countByProjectIds(projectIds: Collection<UUID>): Map<UUID, Int> {
        if (projectIds.isEmpty()) return emptyMap()
        val counted = repository.countByProjectIds(projectIds).associate { it.projectId to it.memberCount.toInt() }
        return projectIds.associateWith { counted[it] ?: 0 }
    }

    override fun delete(id: UUID) = repository.deleteById(id)
}

private fun ProjectMember.toJpaEntity() =
    ProjectMemberJpaEntity(
        id = this.id,
        projectId = this.projectId,
        userId = this.userId,
        roleId = this.roleId,
        assignedAt = this.assignedAt,
        version = this.version,
    )

internal fun ProjectMemberJpaEntity.toDomain() =
    ProjectMember(
        id = this.id,
        projectId = this.projectId,
        userId = this.userId,
        roleId = this.roleId,
        assignedAt = this.assignedAt,
        version = this.version,
    )
