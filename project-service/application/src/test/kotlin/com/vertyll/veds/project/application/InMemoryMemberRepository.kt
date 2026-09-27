package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.repository.ProjectMemberRepository
import java.util.UUID

internal class InMemoryMemberRepository : ProjectMemberRepository {
    val stored = mutableListOf<ProjectMember>()

    fun given(vararg members: ProjectMember) = members.forEach { stored += it }

    override fun save(member: ProjectMember) =
        member.also {
            stored.removeAll { existing -> existing.id == it.id }
            stored += it
        }

    override fun findById(id: UUID) = stored.firstOrNull { it.id == id }

    override fun findByProjectIdAndUserId(
        projectId: UUID,
        userId: UUID,
    ) = stored.firstOrNull { it.projectId == projectId && it.userId == userId }

    override fun findAllByProjectId(projectId: UUID) = stored.filter { it.projectId == projectId }

    override fun findAllByUserId(userId: UUID) = stored.filter { it.userId == userId }

    override fun countByProjectIds(projectIds: Collection<UUID>) =
        stored.filter { it.projectId in projectIds }.groupingBy { it.projectId }.eachCount()

    override fun delete(id: UUID) {
        stored.removeAll { it.id == id }
    }
}
