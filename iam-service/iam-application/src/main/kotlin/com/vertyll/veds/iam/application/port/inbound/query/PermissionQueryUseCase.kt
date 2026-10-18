package com.vertyll.veds.iam.application.port.inbound.query

import com.vertyll.veds.iam.application.dto.PermissionModuleResponse
import com.vertyll.veds.iam.application.dto.PermissionResponse

interface PermissionQueryUseCase {
    fun getAllPermissions(): List<PermissionResponse>

    fun getPermissionsByModule(): List<PermissionModuleResponse>
}
