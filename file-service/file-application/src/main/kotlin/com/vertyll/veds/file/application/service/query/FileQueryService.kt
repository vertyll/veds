package com.vertyll.veds.file.application.service.query

import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.application.dto.DownloadTicketResponse
import com.vertyll.veds.file.application.dto.FileResponse
import com.vertyll.veds.file.application.port.inbound.query.FileQueryUseCase
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
import com.vertyll.veds.file.domain.error.FileError
import com.vertyll.veds.file.domain.model.StoredFile
import com.vertyll.veds.file.domain.repository.StoredFileRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

private const val FILE_ID_PARAM = "fileId"

class FileQueryService(
    private val fileRepository: StoredFileRepository,
    private val storage: ObjectStoragePort,
) : FileQueryUseCase {
    override fun getFile(
        fileId: UUID,
        actor: Actor,
    ): FileResponse = FileResponse.from(requireReadableFile(fileId, actor))

    override fun requestDownload(
        fileId: UUID,
        actor: Actor,
    ): DownloadTicketResponse {
        val file = requireReadableFile(fileId, actor)

        if (!file.isAvailable) {
            throw ApiException(FileError.FILE_NOT_AVAILABLE, mapOf(FILE_ID_PARAM to file.id.toString()))
        }

        val presigned =
            storage.presignDownload(
                objectKey = file.objectKey,
                originalName = file.originalName,
                contentType = file.contentType,
            )

        return DownloadTicketResponse(
            fileId = file.id,
            downloadUrl = presigned.url,
            originalName = file.originalName,
            contentType = file.contentType,
            expiresAt = presigned.expiresAt,
        )
    }

    override fun listForScope(
        scopeId: UUID,
        actor: Actor,
    ): List<FileResponse> =
        fileRepository
            .findAllByScopeId(scopeId)
            .filter { it.isAvailable && it.isOwnedBy(actor.id) }
            .map(FileResponse::from)

    private fun requireReadableFile(
        fileId: UUID,
        actor: Actor,
    ): StoredFile {
        val file =
            fileRepository.findById(fileId)
                ?: throw ApiException(FileError.FILE_NOT_FOUND, mapOf(FILE_ID_PARAM to fileId.toString()))

        if (!file.isOwnedBy(actor.id)) {
            throw ApiException(FileError.FILE_NOT_FOUND, mapOf(FILE_ID_PARAM to fileId.toString()))
        }
        return file
    }
}
