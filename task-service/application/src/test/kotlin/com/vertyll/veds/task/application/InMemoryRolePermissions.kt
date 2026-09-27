package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository

internal class InMemoryRolePermissions : RolePermissionsRepository {
    private val stored = linkedMapOf<String, RolePermissionsRef>()

    init {
        stockRole("MANAGER", TaskPermission.entries.toSet())
        stockRole(
            "MEMBER",
            setOf(
                TaskPermission.VIEW_TASKS,
                TaskPermission.MANAGE_TASKS,
                TaskPermission.COMMENT,
                TaskPermission.LOG_WORK,
            ),
        )
        stockRole("CLIENT", setOf(TaskPermission.VIEW_TASKS, TaskPermission.COMMENT))
    }

    fun stockRole(
        name: String,
        granted: Set<TaskPermission>,
    ) = save(RolePermissionsRef(roleName = name, permissions = granted.mapTo(mutableSetOf()) { it.name }))

    override fun save(role: RolePermissionsRef) = role.also { stored[it.roleName] = it }

    override fun findByName(roleName: String) = stored[roleName]

    override fun findAll() = stored.values.toList()

    override fun deleteByName(roleName: String) {
        stored.remove(roleName)
    }
}
