package com.vertyll.veds.project.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "project_member",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_project_member_project_user", columnNames = ["project_id", "user_id"]),
    ],
    indexes = [
        Index(name = "idx_project_member_project_id", columnList = "project_id"),
        Index(name = "idx_project_member_user_id", columnList = "user_id"),
    ],
)
internal class ProjectMemberJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @Column(name = "user_id", nullable = false)
    var userId: UUID,
    @Column(name = "project_role_id", nullable = false)
    var roleId: UUID,
    @Column(name = "assigned_at", nullable = false)
    var assignedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
