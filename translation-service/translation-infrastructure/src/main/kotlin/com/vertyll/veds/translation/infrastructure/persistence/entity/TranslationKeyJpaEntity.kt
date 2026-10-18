package com.vertyll.veds.translation.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "translation_key",
    indexes = [Index(name = "idx_translation_key_source", columnList = "source_service")],
)
internal class TranslationKeyJpaEntity(
    @Id
    @Column(name = "translation_key", nullable = false, updatable = false, length = 255)
    var key: String,
    @Column(name = "source_service", nullable = false, length = 64)
    var sourceService: String,
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
