package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import java.time.Instant
import java.util.UUID

internal class InMemoryInvitationRepository : ProjectInvitationRepository {
    val stored = linkedMapOf<UUID, ProjectInvitation>()

    fun given(vararg invitations: ProjectInvitation) = invitations.forEach { stored[it.id] = it }

    override fun save(invitation: ProjectInvitation) = invitation.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun findAllByInviteeEmail(inviteeEmail: String) =
        stored.values.filter { it.inviteeEmail.equals(inviteeEmail, ignoreCase = true) }

    override fun findPendingByProjectIdAndEmail(
        projectId: UUID,
        inviteeEmail: String,
    ) = stored.values.firstOrNull {
        it.projectId == projectId && it.inviteeEmail.equals(inviteeEmail, ignoreCase = true) && it.isPending
    }

    override fun findAllPendingExpiredBefore(now: Instant) = stored.values.filter { it.hasExpiredAt(now) }

    override fun countByProjectIdAndStatus(
        projectId: UUID,
        status: InvitationStatus,
    ) = stored.values.count { it.projectId == projectId && it.status == status }.toLong()
}
