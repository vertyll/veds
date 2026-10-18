package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.domain.model.InvitationStatus
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectInvitationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
internal interface ProjectInvitationJpaRepository : JpaRepository<ProjectInvitationJpaEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<ProjectInvitationJpaEntity>

    fun findAllByInviteeEmailIgnoreCase(inviteeEmail: String): List<ProjectInvitationJpaEntity>

    fun findByProjectIdAndInviteeEmailIgnoreCaseAndStatus(
        projectId: UUID,
        inviteeEmail: String,
        status: InvitationStatus,
    ): Optional<ProjectInvitationJpaEntity>

    fun findAllByStatusAndExpiresAtBefore(
        status: InvitationStatus,
        now: Instant,
    ): List<ProjectInvitationJpaEntity>

    fun countByProjectIdAndStatus(
        projectId: UUID,
        status: InvitationStatus,
    ): Long
}
