package com.vertyll.veds.project.infrastructure.persistence.entity

import com.vertyll.veds.project.domain.model.InvitationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "project_invitation",
    indexes = [
        Index(name = "idx_project_invitation_project_id", columnList = "project_id"),
        Index(name = "idx_project_invitation_email", columnList = "invitee_email"),
        Index(name = "idx_project_invitation_status", columnList = "status"),
    ],
)
internal class ProjectInvitationJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @Column(name = "invitee_email", nullable = false)
    var inviteeEmail: String,
    @Column(name = "invitee_id")
    var inviteeId: UUID? = null,
    @Column(name = "inviter_id", nullable = false)
    var inviterId: UUID,
    @Column(name = "project_role_id", nullable = false)
    var roleId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: InvitationStatus = InvitationStatus.PENDING,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
