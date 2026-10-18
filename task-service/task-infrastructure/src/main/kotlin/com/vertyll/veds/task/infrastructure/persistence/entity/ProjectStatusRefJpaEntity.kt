package com.vertyll.veds.task.infrastructure.persistence.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "project_status_ref",
    indexes = [Index(name = "idx_status_ref_project", columnList = "project_id")],
)
internal class ProjectStatusRefJpaEntity(
    @Id
    @Column(name = "status_id", nullable = false, updatable = false)
    var statusId: UUID,
    @Column(name = "project_id", nullable = false)
    var projectId: UUID,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_status_ref_name", joinColumns = [JoinColumn(name = "status_id")])
    @MapKeyColumn(name = "language", length = 8)
    @Column(name = "name", nullable = false)
    var names: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "color", nullable = false, length = 32)
    var color: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
