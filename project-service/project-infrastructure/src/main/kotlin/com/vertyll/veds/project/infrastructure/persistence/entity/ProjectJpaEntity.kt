package com.vertyll.veds.project.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "project",
    indexes = [
        Index(name = "idx_project_owner_id", columnList = "owner_id"),
        Index(name = "idx_project_type_id", columnList = "type_id"),
        Index(name = "idx_project_is_active", columnList = "is_active"),
    ],
)
internal class ProjectJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false,
    @Column(name = "icon_file_id")
    var iconFileId: UUID? = null,
    @Column(name = "type_id")
    var typeId: UUID? = null,
    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID,
    @Column(name = "hidden_work_log_enabled", nullable = false)
    var hiddenWorkLogEnabled: Boolean = false,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
