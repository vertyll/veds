package com.vertyll.veds.task.domain.repository

import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import java.util.UUID

interface ProjectDirectoryRepository {
    fun saveProject(project: ProjectRef): ProjectRef

    fun findProject(projectId: UUID): ProjectRef?

    fun saveCategory(category: ProjectCategoryRef): ProjectCategoryRef

    fun removeCategory(categoryId: UUID)

    fun findCategories(projectId: UUID): List<ProjectCategoryRef>

    fun saveStatus(status: ProjectStatusRef): ProjectStatusRef

    fun removeStatus(statusId: UUID)

    fun findStatuses(projectId: UUID): List<ProjectStatusRef>

    fun saveMembership(membership: ProjectMembershipRef): ProjectMembershipRef

    fun removeMembership(
        projectId: UUID,
        userId: UUID,
    )

    fun findMembership(
        projectId: UUID,
        userId: UUID,
    ): ProjectMembershipRef?

    fun findMemberships(projectId: UUID): List<ProjectMembershipRef>
}
