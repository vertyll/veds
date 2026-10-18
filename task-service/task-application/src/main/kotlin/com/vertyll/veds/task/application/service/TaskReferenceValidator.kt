package com.vertyll.veds.task.application.service

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import java.util.UUID

class TaskReferenceValidator(
    private val projectDirectory: ProjectDirectoryRepository,
) {
    fun validate(
        projectId: UUID,
        statusId: UUID?,
        categoryIds: Set<UUID>,
        assigneeIds: Set<UUID>,
    ) {
        statusId?.let { requested ->
            val known = projectDirectory.findStatuses(projectId).any { it.statusId == requested }
            if (!known) {
                throw ApiException(TaskError.STATUS_NOT_IN_PROJECT, mapOf("statusId" to requested.toString()))
            }
        }

        if (categoryIds.isNotEmpty()) {
            val known = projectDirectory.findCategories(projectId).map { it.categoryId }.toSet()
            val unknown = categoryIds - known
            if (unknown.isNotEmpty()) {
                throw ApiException(
                    TaskError.CATEGORY_NOT_IN_PROJECT,
                    mapOf("categoryIds" to unknown.map { it.toString() }),
                )
            }
        }

        if (assigneeIds.isNotEmpty()) {
            val members = projectDirectory.findMemberships(projectId).map { it.userId }.toSet()
            val outsiders = assigneeIds - members
            if (outsiders.isNotEmpty()) {
                throw ApiException(
                    TaskError.ASSIGNEE_NOT_A_MEMBER,
                    mapOf("userIds" to outsiders.map { it.toString() }),
                )
            }
        }
    }
}
