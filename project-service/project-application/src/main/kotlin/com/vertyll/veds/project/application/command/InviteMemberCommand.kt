package com.vertyll.veds.project.application.command

import java.util.UUID

data class InviteMemberCommand(
    val email: String,
    val roleId: UUID?,
)
