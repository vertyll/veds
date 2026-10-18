package com.vertyll.veds.task.application.service

import com.vertyll.veds.task.application.port.inbound.ProjectProjectionUseCase
import com.vertyll.veds.task.application.port.outbound.UseCaseLogger
import com.vertyll.veds.task.domain.model.ProjectCategoryRef
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.ProjectStatusRef
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import java.util.UUID

class ProjectProjectionService(
    private val projectDirectory: ProjectDirectoryRepository,
    private val taskRepository: TaskRepository,
    private val logger: UseCaseLogger,
) : ProjectProjectionUseCase {
    override fun projectChanged(project: ProjectRef) {
        projectDirectory.saveProject(project)
    }

    override fun projectArchived(projectId: UUID) {
        val existing = projectDirectory.findProject(projectId)
        if (existing == null) {
            logger.debug("Ignoring archival of unknown project {}", projectId)
            return
        }
        projectDirectory.saveProject(existing.copy(isActive = false))
    }

    override fun categoryChanged(category: ProjectCategoryRef) {
        projectDirectory.saveCategory(category)
    }

    override fun categoryRemoved(categoryId: UUID) {
        val affected = taskRepository.findAllByCategoryId(categoryId)
        if (affected.isNotEmpty()) {
            taskRepository.saveAll(affected.map { it.withoutCategory(categoryId) })
            logger.info("Dropped removed category {} from {} tasks", categoryId, affected.size)
        }
        projectDirectory.removeCategory(categoryId)
    }

    override fun statusChanged(status: ProjectStatusRef) {
        projectDirectory.saveStatus(status)
    }

    override fun statusRemoved(statusId: UUID) {
        val affected = taskRepository.findAllByStatusId(statusId)
        if (affected.isNotEmpty()) {
            taskRepository.saveAll(affected.map { it.withoutStatus(statusId) })
            logger.info("Cleared removed status {} on {} tasks", statusId, affected.size)
        }
        projectDirectory.removeStatus(statusId)
    }

    override fun memberJoined(membership: ProjectMembershipRef) {
        projectDirectory.saveMembership(membership)
    }

    override fun memberRemoved(
        projectId: UUID,
        userId: UUID,
    ) {
        projectDirectory.removeMembership(projectId, userId)
    }
}
