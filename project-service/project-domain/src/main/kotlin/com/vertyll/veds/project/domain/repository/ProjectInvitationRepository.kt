package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import java.time.Instant
import java.util.UUID

interface ProjectInvitationRepository {
    fun save(invitation: ProjectInvitation): ProjectInvitation

    fun findById(id: UUID): ProjectInvitation?

    fun findAllByProjectId(projectId: UUID): List<ProjectInvitation>

    fun findAllByInviteeEmail(inviteeEmail: String): List<ProjectInvitation>

    fun findPendingByProjectIdAndEmail(
        projectId: UUID,
        inviteeEmail: String,
    ): ProjectInvitation?

    fun findAllPendingExpiredBefore(now: Instant): List<ProjectInvitation>

    fun countByProjectIdAndStatus(
        projectId: UUID,
        status: InvitationStatus,
    ): Long
}
