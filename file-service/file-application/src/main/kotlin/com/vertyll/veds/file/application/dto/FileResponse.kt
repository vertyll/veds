package com.vertyll.veds.file.application.dto

import com.vertyll.veds.file.domain.model.FileScope
import com.vertyll.veds.file.domain.model.StoredFile
import com.vertyll.veds.file.domain.model.UploadStatus
import java.time.Instant
import java.util.UUID

data class FileResponse(
    val id: UUID,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val status: UploadStatus,
    val scope: FileScope,
    val scopeId: UUID?,
    val createdAt: Instant,
) {
    companion object {
        fun from(file: StoredFile): FileResponse =
            FileResponse(
                id = file.id,
                originalName = file.originalName,
                contentType = file.contentType,
                sizeBytes = file.sizeBytes,
                status = file.status,
                scope = file.scope,
                scopeId = file.scopeId,
                createdAt = file.createdAt,
            )
    }
}
