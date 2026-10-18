package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.RoleResponse
import com.vertyll.veds.iam.domain.model.RoleScope

interface RoleQueryUseCase {
    fun getRoleById(id: Long): RoleResponse

    fun getRoleByName(name: String): RoleResponse

    fun getAllRoles(): List<RoleResponse>

    fun getRolesInScope(scope: RoleScope): List<RoleResponse>

    fun getRolesForUser(userId: Long): List<RoleResponse>
}
