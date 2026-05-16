package com.vertyll.veds.iam.application.port.inbound

import com.vertyll.veds.iam.application.dto.RoleResponse

interface RoleUseCase {
    fun getRoleById(id: Long): RoleResponse

    fun getRoleByName(name: String): RoleResponse

    fun getAllRoles(): List<RoleResponse>

    fun getRolesForUser(userId: Long): List<RoleResponse>

    fun assignRoleToUser(
        userId: Long,
        roleName: String,
        version: Long? = null,
    )

    fun removeRoleFromUser(
        userId: Long,
        roleName: String,
        version: Long? = null,
    )
}
