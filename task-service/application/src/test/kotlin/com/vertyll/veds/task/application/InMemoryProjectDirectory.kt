package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import java.util.UUID

internal class InMemoryProjectDirectory : ProjectDirectoryRepository {
    val projects = linkedMapOf<UUID, ProjectRef>()
    val categories = linkedMapOf<UUID, ProjectCategoryRef>()
    val statuses = linkedMapOf<UUID, ProjectStatusRef>()
    val memberships = mutableListOf<ProjectMembershipRef>()

    override fun saveProject(project: ProjectRef) = project.also { projects[it.projectId] = it }

    override fun findProject(projectId: UUID) = projects[projectId]

    override fun saveCategory(category: ProjectCategoryRef) = category.also { categories[it.categoryId] = it }

    override fun removeCategory(categoryId: UUID) {
        categories.remove(categoryId)
    }

    override fun findCategories(projectId: UUID) = categories.values.filter { it.projectId == projectId }

    override fun saveStatus(status: ProjectStatusRef) = status.also { statuses[it.statusId] = it }

    override fun removeStatus(statusId: UUID) {
        statuses.remove(statusId)
    }

    override fun findStatuses(projectId: UUID) = statuses.values.filter { it.projectId == projectId }

    override fun saveMembership(membership: ProjectMembershipRef) =
        membership.also {
            memberships.removeAll { existing -> existing.projectId == it.projectId && existing.userId == it.userId }
            memberships += it
        }

    override fun removeMembership(
        projectId: UUID,
        userId: UUID,
    ) {
        memberships.removeAll { it.projectId == projectId && it.userId == userId }
    }

    override fun findMembership(
        projectId: UUID,
        userId: UUID,
    ) = memberships.firstOrNull { it.projectId == projectId && it.userId == userId }

    override fun findMemberships(projectId: UUID) = memberships.filter { it.projectId == projectId }
}
