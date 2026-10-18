package com.vertyll.veds.file.infrastructure.persistence.repository

import com.vertyll.veds.file.domain.model.UploadStatus
import com.vertyll.veds.file.infrastructure.persistence.entity.StoredFileJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
internal interface StoredFileJpaRepository : JpaRepository<StoredFileJpaEntity, UUID> {
    fun findAllByScopeId(scopeId: UUID): List<StoredFileJpaEntity>

    fun findAllByStatusAndCreatedAtBefore(
        status: UploadStatus,
        createdBefore: Instant,
    ): List<StoredFileJpaEntity>

    fun findAllByStatus(status: UploadStatus): List<StoredFileJpaEntity>
}
