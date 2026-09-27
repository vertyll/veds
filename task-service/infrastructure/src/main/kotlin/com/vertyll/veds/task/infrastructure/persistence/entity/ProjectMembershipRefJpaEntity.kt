package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "project_membership_ref",
    indexes = [Index(name = "idx_membership_ref_user", columnList = "user_id")],
)
@IdClass(ProjectMembershipRefId::class)
internal class ProjectMembershipRefJpaEntity(
    @Id
    @Column(name = "project_id", nullable = false, updatable = false)
    var projectId: UUID,
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,
    @Column(name = "role_code", nullable = false, length = 32)
    var roleCode: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
