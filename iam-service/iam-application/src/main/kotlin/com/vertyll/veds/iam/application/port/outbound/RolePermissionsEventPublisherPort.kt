package com.vertyll.veds.iam.application.port.outbound

import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleScope

interface RolePermissionsEventPublisherPort {
    fun publishChanged(role: Role)

    fun publishRemoved(
        roleName: String,
        scope: RoleScope,
    )
}
