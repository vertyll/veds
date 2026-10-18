package com.vertyll.veds.translation.infrastructure.persistence.entity

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
    name = "translation_value",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_translation_value_key_language", columnNames = ["translation_key", "language"]),
    ],
    indexes = [
        Index(name = "idx_translation_value_language", columnList = "language"),
        Index(name = "idx_translation_value_updated", columnList = "language, updated_at"),
    ],
)
internal class TranslationValueJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "translation_key", nullable = false, length = 255)
    var key: String,
    @Column(name = "language", nullable = false, length = 16)
    var language: String,
    @Column(name = "default_value", columnDefinition = "TEXT")
    var defaultValue: String? = null,
    @Column(name = "override_value", columnDefinition = "TEXT")
    var overrideValue: String? = null,
    @Column(name = "updated_by")
    var updatedBy: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
