package com.vertyll.veds.project.infrastructure.web.dto

import com.vertyll.veds.project.application.command.InviteMemberCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class InviteMemberRequest(
    @field:NotBlank(message = "validation.project.invitee_email_required")
    @field:Email(message = "validation.project.invitee_email_invalid")
    val email: String = "",
    val roleId: UUID? = null,
) {
    fun toCommand(): InviteMemberCommand = InviteMemberCommand(email = email, roleId = roleId)
}
