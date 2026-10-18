package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.domain.model.ProjectInvitation
import com.vertyll.veds.project.domain.repository.ProjectInvitationRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectInvitationJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectInvitationJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
internal class ProjectInvitationPersistenceAdapter(
    private val repository: ProjectInvitationJpaRepository,
) : ProjectInvitationRepository {
    override fun save(invitation: ProjectInvitation): ProjectInvitation = repository.save(invitation.toJpaEntity()).toDomain()

    override fun findById(id: UUID): ProjectInvitation? = repository.findByIdOrNull(id)?.toDomain()

    override fun findAllByProjectId(projectId: UUID): List<ProjectInvitation> =
        repository.findAllByProjectId(projectId).map { it.toDomain() }

    override fun findAllByInviteeEmail(inviteeEmail: String): List<ProjectInvitation> =
        repository.findAllByInviteeEmailIgnoreCase(inviteeEmail).map { it.toDomain() }

    override fun findPendingByProjectIdAndEmail(
        projectId: UUID,
        inviteeEmail: String,
    ): ProjectInvitation? =
        repository
            .findByProjectIdAndInviteeEmailIgnoreCaseAndStatus(projectId, inviteeEmail, InvitationStatus.PENDING)
            .orElse(null)
            ?.toDomain()

    override fun findAllPendingExpiredBefore(now: Instant): List<ProjectInvitation> =
        repository.findAllByStatusAndExpiresAtBefore(InvitationStatus.PENDING, now).map { it.toDomain() }

    override fun countByProjectIdAndStatus(
        projectId: UUID,
        status: InvitationStatus,
    ): Long = repository.countByProjectIdAndStatus(projectId, status)
}

private fun ProjectInvitation.toJpaEntity() =
    ProjectInvitationJpaEntity(
        id = this.id,
        projectId = this.projectId,
        inviteeEmail = this.inviteeEmail,
        inviteeId = this.inviteeId,
        inviterId = this.inviterId,
        roleId = this.roleId,
        status = this.status,
        expiresAt = this.expiresAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun ProjectInvitationJpaEntity.toDomain() =
    ProjectInvitation(
        id = this.id,
        projectId = this.projectId,
        inviteeEmail = this.inviteeEmail,
        inviteeId = this.inviteeId,
        inviterId = this.inviterId,
        roleId = this.roleId,
        status = this.status,
        expiresAt = this.expiresAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
