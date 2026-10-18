package com.vertyll.veds.project.infrastructure.web.dto

import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import java.util.UUID

data class UpdateMemberRoleRequest(
    val roleId: UUID,
) {
    fun toCommand(): UpdateMemberRoleCommand = UpdateMemberRoleCommand(roleId = roleId)
}
