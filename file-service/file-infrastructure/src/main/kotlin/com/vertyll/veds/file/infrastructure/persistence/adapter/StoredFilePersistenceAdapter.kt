package com.vertyll.veds.file.infrastructure.persistence.adapter

import com.vertyll.veds.file.domain.model.StoredFile
import com.vertyll.veds.file.domain.model.UploadStatus
import com.vertyll.veds.file.domain.repository.StoredFileRepository
import com.vertyll.veds.file.infrastructure.persistence.entity.StoredFileJpaEntity
import com.vertyll.veds.file.infrastructure.persistence.repository.StoredFileJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
internal class StoredFilePersistenceAdapter(
    private val repository: StoredFileJpaRepository,
) : StoredFileRepository {
    override fun save(file: StoredFile): StoredFile = repository.save(file.toEntity()).toDomain()

    override fun saveAll(files: Collection<StoredFile>): List<StoredFile> =
        repository.saveAll(files.map { it.toEntity() }).map { it.toDomain() }

    override fun findById(id: UUID): StoredFile? = repository.findByIdOrNull(id)?.toDomain()

    override fun findAllByIds(ids: Collection<UUID>): List<StoredFile> =
        if (ids.isEmpty()) emptyList() else repository.findAllById(ids).map { it.toDomain() }

    override fun findAllByScopeId(scopeId: UUID): List<StoredFile> = repository.findAllByScopeId(scopeId).map { it.toDomain() }

    override fun findAbandoned(
        status: UploadStatus,
        createdBefore: Instant,
    ): List<StoredFile> = repository.findAllByStatusAndCreatedAtBefore(status, createdBefore).map { it.toDomain() }

    override fun findAllDeleted(): List<StoredFile> = repository.findAllByStatus(UploadStatus.DELETED).map { it.toDomain() }

    override fun delete(id: UUID) = repository.deleteById(id)
}

private fun StoredFile.toEntity() =
    StoredFileJpaEntity(
        id = id,
        objectKey = objectKey,
        originalName = originalName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        status = status,
        ownerId = ownerId,
        scope = scope,
        scopeId = scopeId,
        createdAt = createdAt,
        confirmedAt = confirmedAt,
        version = version,
    )

internal fun StoredFileJpaEntity.toDomain() =
    StoredFile(
        id = id,
        objectKey = objectKey,
        originalName = originalName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        status = status,
        ownerId = ownerId,
        scope = scope,
        scopeId = scopeId,
        createdAt = createdAt,
        confirmedAt = confirmedAt,
        version = version,
    )
