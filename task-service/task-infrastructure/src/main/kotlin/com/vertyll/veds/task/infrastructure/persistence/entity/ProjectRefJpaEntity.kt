package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "project_ref")
internal class ProjectRefJpaEntity(
    @Id
    @Column(name = "project_id", nullable = false, updatable = false)
    var projectId: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "hidden_work_log_enabled", nullable = false)
    var hiddenWorkLogEnabled: Boolean = false,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
