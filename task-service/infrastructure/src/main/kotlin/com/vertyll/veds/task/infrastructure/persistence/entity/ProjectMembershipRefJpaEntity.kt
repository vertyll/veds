package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.time.Instant
import java.util.UUID

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
