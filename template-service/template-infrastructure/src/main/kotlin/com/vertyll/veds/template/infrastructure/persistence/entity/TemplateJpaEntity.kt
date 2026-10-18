package com.vertyll.veds.template.infrastructure.persistence.entity

import com.vertyll.veds.template.domain.model.TemplateStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "template")
internal class TemplateJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TemplateStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
