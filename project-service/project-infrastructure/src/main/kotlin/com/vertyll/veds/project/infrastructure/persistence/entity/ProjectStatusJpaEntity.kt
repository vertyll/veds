package com.vertyll.veds.project.infrastructure.persistence.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "project_status",
    indexes = [Index(name = "idx_project_status_project_id", columnList = "project_id")],
)
internal class ProjectStatusJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @Column(name = "color", nullable = false, length = 32)
    var color: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "project_status_translation",
        joinColumns = [JoinColumn(name = "project_status_id")],
    )
    var translations: MutableSet<TranslationEmbeddable> = mutableSetOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
