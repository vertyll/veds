package com.vertyll.veds.file.infrastructure.persistence.entity

import com.vertyll.veds.file.domain.model.FileScope
import com.vertyll.veds.file.domain.model.UploadStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "stored_file",
    indexes = [
        Index(name = "idx_stored_file_owner", columnList = "owner_id"),
        Index(name = "idx_stored_file_scope", columnList = "scope_id"),
        Index(name = "idx_stored_file_status", columnList = "status, created_at"),
    ],
)
internal class StoredFileJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    var objectKey: String,
    @Column(name = "original_name", nullable = false, length = 512)
    var originalName: String,
    @Column(name = "content_type", nullable = false, length = 255)
    var contentType: String,
    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: UploadStatus = UploadStatus.PENDING,
    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 64)
    var scope: FileScope,
    @Column(name = "scope_id")
    var scopeId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null,
    @Version
    @Column(name = "version")
    var version: Long? = null,
)
