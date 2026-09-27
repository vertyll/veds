package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.application.port.outbound.RolePermissionsEventPublisherPort
import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.model.RoleScope

internal class RecordingRolePermissionsPublisher : RolePermissionsEventPublisherPort {
    val changed = mutableListOf<Role>()
    val removed = mutableListOf<String>()

    override fun publishChanged(role: Role) {
        changed += role
    }

    override fun publishRemoved(
        roleName: String,
        scope: RoleScope,
    ) {
        removed += roleName
    }
}
