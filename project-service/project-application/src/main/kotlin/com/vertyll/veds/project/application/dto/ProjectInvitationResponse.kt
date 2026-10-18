package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import java.time.Instant
import java.util.UUID

data class ProjectInvitationResponse(
    val id: UUID,
    val projectId: UUID,
    val projectName: String?,
    val inviteeEmail: String,
    val inviterId: UUID,
    val roleId: UUID,
    val status: InvitationStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    val version: Long?,
) {
    companion object {
        fun from(
            invitation: ProjectInvitation,
            projectName: String?,
        ): ProjectInvitationResponse =
            ProjectInvitationResponse(
                id = invitation.id,
                projectId = invitation.projectId,
                projectName = projectName,
                inviteeEmail = invitation.inviteeEmail,
                inviterId = invitation.inviterId,
                roleId = invitation.roleId,
                status = invitation.status,
                expiresAt = invitation.expiresAt,
                createdAt = invitation.createdAt,
                version = invitation.version,
            )
    }
}
