package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectCategoryRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectMembershipRefId
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectMembershipRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.entity.ProjectStatusRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.ProjectCategoryRefJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.ProjectMembershipRefJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.ProjectRefJpaRepository
import com.vertyll.veds.task.infrastructure.persistence.repository.ProjectStatusRefJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
internal class ProjectDirectoryPersistenceAdapter(
    private val projects: ProjectRefJpaRepository,
    private val categories: ProjectCategoryRefJpaRepository,
    private val statuses: ProjectStatusRefJpaRepository,
    private val memberships: ProjectMembershipRefJpaRepository,
) : ProjectDirectoryRepository {
    override fun saveProject(project: ProjectRef): ProjectRef =
        projects
            .save(
                ProjectRefJpaEntity(
                    projectId = project.projectId,
                    name = project.name,
                    isActive = project.isActive,
                    hiddenWorkLogEnabled = project.hiddenWorkLogEnabled,
                    updatedAt = project.updatedAt,
                ),
            ).toDomain()

    override fun findProject(projectId: UUID): ProjectRef? = projects.findByIdOrNull(projectId)?.toDomain()

    override fun saveCategory(category: ProjectCategoryRef): ProjectCategoryRef =
        categories
            .save(
                ProjectCategoryRefJpaEntity(
                    categoryId = category.categoryId,
                    projectId = category.projectId,
                    names = category.names.toMutableMap(),
                    color = category.color,
                    updatedAt = category.updatedAt,
                ),
            ).toDomain()

    override fun removeCategory(categoryId: UUID) = categories.deleteById(categoryId)

    override fun findCategories(projectId: UUID): List<ProjectCategoryRef> = categories.findAllByProjectId(projectId).map { it.toDomain() }

    override fun saveStatus(status: ProjectStatusRef): ProjectStatusRef =
        statuses
            .save(
                ProjectStatusRefJpaEntity(
                    statusId = status.statusId,
                    projectId = status.projectId,
                    names = status.names.toMutableMap(),
                    color = status.color,
                    updatedAt = status.updatedAt,
                ),
            ).toDomain()

    override fun removeStatus(statusId: UUID) = statuses.deleteById(statusId)

    override fun findStatuses(projectId: UUID): List<ProjectStatusRef> = statuses.findAllByProjectId(projectId).map { it.toDomain() }

    override fun saveMembership(membership: ProjectMembershipRef): ProjectMembershipRef =
        memberships
            .save(
                ProjectMembershipRefJpaEntity(
                    projectId = membership.projectId,
                    userId = membership.userId,
                    roleCode = membership.roleCode,
                    updatedAt = membership.updatedAt,
                ),
            ).toDomain()

    @Transactional
    override fun removeMembership(
        projectId: UUID,
        userId: UUID,
    ) = memberships.deleteByProjectIdAndUserId(projectId, userId)

    override fun findMembership(
        projectId: UUID,
        userId: UUID,
    ): ProjectMembershipRef? = memberships.findByIdOrNull(ProjectMembershipRefId(projectId, userId))?.toDomain()

    override fun findMemberships(projectId: UUID): List<ProjectMembershipRef> =
        memberships.findAllByProjectId(projectId).map { it.toDomain() }
}

private fun ProjectRefJpaEntity.toDomain() =
    ProjectRef(
        projectId = projectId,
        name = name,
        isActive = isActive,
        hiddenWorkLogEnabled = hiddenWorkLogEnabled,
        updatedAt = updatedAt,
    )

private fun ProjectCategoryRefJpaEntity.toDomain() =
    ProjectCategoryRef(
        categoryId = categoryId,
        projectId = projectId,
        names = names.toMap(),
        color = color,
        updatedAt = updatedAt,
    )

private fun ProjectStatusRefJpaEntity.toDomain() =
    ProjectStatusRef(
        statusId = statusId,
        projectId = projectId,
        names = names.toMap(),
        color = color,
        updatedAt = updatedAt,
    )

private fun ProjectMembershipRefJpaEntity.toDomain() =
    ProjectMembershipRef(projectId = projectId, userId = userId, roleCode = roleCode, updatedAt = updatedAt)
