package com.vertyll.veds.file.domain.repository

import com.vertyll.veds.file.domain.model.StoredFile
import com.vertyll.veds.file.domain.model.UploadStatus
import java.time.Instant
import java.util.UUID

interface StoredFileRepository {
    fun save(file: StoredFile): StoredFile

    fun saveAll(files: Collection<StoredFile>): List<StoredFile>

    fun findById(id: UUID): StoredFile?

    fun findAllByIds(ids: Collection<UUID>): List<StoredFile>

    fun findAllByScopeId(scopeId: UUID): List<StoredFile>

    fun findAbandoned(
        status: UploadStatus,
        createdBefore: Instant,
    ): List<StoredFile>

    fun findAllDeleted(): List<StoredFile>

    fun delete(id: UUID)
}
