package com.vertyll.veds.project.application.port.inbound.command

import com.vertyll.veds.project.application.command.InviteMemberCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.ProjectInvitationResponse
import java.util.UUID

interface ProjectInvitationCommandUseCase {
    fun invite(
        projectId: UUID,
        command: InviteMemberCommand,
        actorId: UUID,
    ): ProjectInvitationResponse

    fun acceptInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse

    fun rejectInvitation(
        invitationId: UUID,
        actor: Actor,
    ): ProjectInvitationResponse

    fun expireOverdueInvitations(): Int
}
