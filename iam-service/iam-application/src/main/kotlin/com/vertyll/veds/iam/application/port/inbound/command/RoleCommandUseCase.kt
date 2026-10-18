package com.vertyll.veds.iam.application.port.inbound.command

import com.vertyll.veds.iam.application.command.CreateRoleCommand
import com.vertyll.veds.iam.application.command.UpdateRoleCommand
import com.vertyll.veds.iam.application.dto.RoleResponse

interface RoleCommandUseCase {
    fun createRole(command: CreateRoleCommand): RoleResponse

    fun updateRole(
        name: String,
        command: UpdateRoleCommand,
        version: Long? = null,
    ): RoleResponse

    fun deleteRole(name: String)

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
