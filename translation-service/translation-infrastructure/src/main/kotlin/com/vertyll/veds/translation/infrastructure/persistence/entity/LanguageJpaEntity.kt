package com.vertyll.veds.translation.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "language")
internal class LanguageJpaEntity(
    @Id
    @Column(name = "tag", nullable = false, updatable = false, length = 16)
    var tag: String,
    @Column(name = "display_name", nullable = false, length = 128)
    var displayName: String,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
