package com.vertyll.veds.task.application.service

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.domain.repository.TaskRepository
import com.vertyll.veds.task.domain.service.AccessDecision
import com.vertyll.veds.task.domain.service.TaskAccessPolicy
import java.util.UUID

private const val PROJECT_ID_PARAM = "projectId"
private const val TASK_ID_PARAM = "taskId"

class TaskAuthorizationService(
    private val projectDirectory: ProjectDirectoryRepository,
    private val taskRepository: TaskRepository,
    private val rolePermissions: RolePermissionsRepository,
) {
    fun requireProjectPermission(
        projectId: UUID,
        actorId: UUID,
        permission: TaskPermission,
    ): ProjectRef {
        val project =
            projectDirectory.findProject(projectId)
                ?: throw ApiException(TaskError.PROJECT_NOT_KNOWN, mapOf(PROJECT_ID_PARAM to projectId.toString()))

        val membership = projectDirectory.findMembership(projectId, actorId)

        return when (val decision = TaskAccessPolicy.evaluate(project, membership, grantsOf(membership), permission)) {
            is AccessDecision.Permit -> project
            is AccessDecision.Deny -> throw ApiException(decision.reason, mapOf(PROJECT_ID_PARAM to projectId.toString()))
        }
    }

    fun requireTaskPermission(
        taskId: UUID,
        actorId: UUID,
        permission: TaskPermission,
    ): Task {
        val task =
            taskRepository.findById(taskId)
                ?: throw ApiException(TaskError.TASK_NOT_FOUND, mapOf(TASK_ID_PARAM to taskId.toString()))

        val project =
            projectDirectory.findProject(task.projectId)
                ?: throw ApiException(TaskError.PROJECT_NOT_KNOWN, mapOf(PROJECT_ID_PARAM to task.projectId.toString()))

        val membership = projectDirectory.findMembership(projectId = task.projectId, userId = actorId)
        val role = grantsOf(membership)

        if (!TaskAccessPolicy.evaluate(project, membership, role, TaskPermission.VIEW_TASKS, task).isPermitted) {
            throw ApiException(TaskError.TASK_NOT_FOUND, mapOf(TASK_ID_PARAM to taskId.toString()))
        }
        if (!TaskAccessPolicy.canSeeRestrictedTask(task, role, actorId)) {
            throw ApiException(TaskError.TASK_NOT_FOUND, mapOf(TASK_ID_PARAM to taskId.toString()))
        }

        return when (val decision = TaskAccessPolicy.evaluate(project, membership, role, permission, task)) {
            is AccessDecision.Permit -> task
            is AccessDecision.Deny -> throw ApiException(decision.reason, mapOf(TASK_ID_PARAM to taskId.toString()))
        }
    }

    fun effectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<TaskPermission> {
        val membership = projectDirectory.findMembership(projectId, actorId)
        return TaskAccessPolicy.permissionsOf(membership, grantsOf(membership))
    }

    private fun grantsOf(membership: ProjectMembershipRef?): RolePermissionsRef? =
        membership?.let { rolePermissions.findByName(it.roleCode) }
}
