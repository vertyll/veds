package com.vertyll.veds.task.domain.service

import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.ProjectMembershipRef
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskPermission
import java.util.UUID

object TaskAccessPolicy {
    private val MUTATING = setOf(TaskPermission.MANAGE_TASKS)

    fun evaluate(
        project: ProjectRef,
        membership: ProjectMembershipRef?,
        role: RolePermissionsRef?,
        permission: TaskPermission,
        task: Task? = null,
    ): AccessDecision {
        if (!project.isActive && permission in MUTATING) {
            return AccessDecision.Deny(TaskError.PROJECT_ARCHIVED)
        }
        if (task != null && !task.isActive && permission in MUTATING) {
            return AccessDecision.Deny(TaskError.TASK_ARCHIVED)
        }

        return if (membership != null && role != null && role.grants(permission)) {
            AccessDecision.Permit
        } else {
            AccessDecision.Deny(TaskError.TASK_ACCESS_DENIED)
        }
    }

    fun permissionsOf(
        membership: ProjectMembershipRef?,
        role: RolePermissionsRef?,
    ): Set<TaskPermission> {
        if (membership == null || role == null) return emptySet()
        return TaskPermission.entries.filterTo(mutableSetOf()) { role.grants(it) }
    }

    fun canSeeRestrictedTask(
        task: Task,
        role: RolePermissionsRef?,
        userId: UUID,
    ): Boolean {
        task.accessRoleId ?: return true
        return task.wasCreatedBy(userId) || task.isAssignedTo(userId) || role?.grants(TaskPermission.VIEW_RESTRICTED_TASKS) == true
    }
}
