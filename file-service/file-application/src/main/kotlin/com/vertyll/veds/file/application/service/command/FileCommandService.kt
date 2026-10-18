@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.file.application.service.command

import com.vertyll.veds.file.application.command.AttachFileCommand
import com.vertyll.veds.file.application.command.ConfirmUploadCommand
import com.vertyll.veds.file.application.command.RequestUploadCommand
import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.application.dto.FileResponse
import com.vertyll.veds.file.application.dto.UploadTicketResponse
import com.vertyll.veds.file.application.port.inbound.command.FileCommandUseCase
import com.vertyll.veds.file.application.port.outbound.FileEventPublisherPort
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
import com.vertyll.veds.file.application.port.outbound.UseCaseLogger
import com.vertyll.veds.file.domain.error.FileError
import com.vertyll.veds.file.domain.model.StoredFile
import com.vertyll.veds.file.domain.model.UploadStatus
import com.vertyll.veds.file.domain.repository.StoredFileRepository
import com.vertyll.veds.sharederror.ApiException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@Suppress("LongParameterList")
class FileCommandService(
    private val fileRepository: StoredFileRepository,
    private val storage: ObjectStoragePort,
    private val eventPublisher: FileEventPublisherPort,
    private val logger: UseCaseLogger,
) : FileCommandUseCase {
    private companion object {
        private val ABANDONED_AFTER: Duration = Duration.ofHours(6)
    }

    override fun requestUpload(
        command: RequestUploadCommand,
        actor: Actor,
    ): UploadTicketResponse {
        if (!command.scope.permits(command.contentType)) {
            throw ApiException(
                FileError.CONTENT_TYPE_NOT_ALLOWED,
                mapOf("contentType" to command.contentType, "scope" to command.scope.name),
            )
        }
        if (!command.scope.permitsSize(command.declaredSizeBytes)) {
            throw ApiException(
                FileError.FILE_TOO_LARGE,
                mapOf("maxBytes" to command.scope.maxSizeBytes, "declaredBytes" to command.declaredSizeBytes),
            )
        }

        val fileId = Uuid.generateV7().toJavaUuid()
        val objectKey = objectKeyFor(command.scope.name, fileId, command.originalName)

        val file =
            fileRepository.save(
                StoredFile
                    .pending(
                        objectKey = objectKey,
                        originalName = command.originalName,
                        contentType = command.contentType,
                        declaredSizeBytes = command.declaredSizeBytes,
                        ownerId = actor.id,
                        scope = command.scope,
                        scopeId = command.scopeId,
                    ).copy(id = fileId),
            )

        val presigned = storage.presignUpload(objectKey, command.contentType)

        return UploadTicketResponse(
            fileId = file.id,
            uploadUrl = presigned.url,
            expiresAt = presigned.expiresAt,
            maxSizeBytes = command.scope.maxSizeBytes,
        )
    }

    override fun confirmUpload(
        command: ConfirmUploadCommand,
        actor: Actor,
    ): FileResponse {
        val file = requireOwnedFile(command.fileId, actor)

        if (file.status != UploadStatus.PENDING) {
            throw ApiException(FileError.UPLOAD_NOT_PENDING, mapOf("fileId" to file.id.toString()))
        }

        val actualSize =
            storage.sizeOf(file.objectKey)
                ?: throw ApiException(FileError.OBJECT_MISSING_IN_STORAGE, mapOf("fileId" to file.id.toString()))

        if (!file.scope.permitsSize(actualSize)) {
            storage.delete(file.objectKey)
            throw ApiException(
                FileError.FILE_TOO_LARGE,
                mapOf("maxBytes" to file.scope.maxSizeBytes, "actualBytes" to actualSize),
            )
        }

        val confirmed = fileRepository.save(file.confirmed(actualSize))
        eventPublisher.publishFileConfirmed(confirmed.id, confirmed.scope, confirmed.scopeId, confirmed.ownerId)

        return FileResponse.from(confirmed)
    }

    override fun attach(
        command: AttachFileCommand,
        actor: Actor,
    ): FileResponse {
        val file = requireOwnedFile(command.fileId, actor)

        if (!file.isAvailable) {
            throw ApiException(FileError.FILE_NOT_AVAILABLE, mapOf("fileId" to file.id.toString()))
        }

        return FileResponse.from(fileRepository.save(file.attachedTo(command.scopeId)))
    }

    override fun delete(
        fileId: UUID,
        actor: Actor,
    ) {
        val file = requireOwnedFile(fileId, actor)
        if (file.status == UploadStatus.DELETED) return

        val deleted = fileRepository.save(file.deleted())
        eventPublisher.publishFileDeleted(deleted.id, deleted.scope, deleted.scopeId)
    }

    override fun purgeAbandonedUploads(): Int {
        val abandoned = fileRepository.findAbandoned(UploadStatus.PENDING, Instant.now().minus(ABANDONED_AFTER))
        if (abandoned.isEmpty()) return 0

        abandoned.forEach { file ->
            storage.delete(file.objectKey)
            fileRepository.delete(file.id)
        }
        logger.info("Purged {} abandoned uploads", abandoned.size)
        return abandoned.size
    }

    override fun purgeDeletedObjects(): Int {
        val deleted = fileRepository.findAllDeleted()
        if (deleted.isEmpty()) return 0

        deleted.forEach { file ->
            storage.delete(file.objectKey)
            fileRepository.delete(file.id)
        }
        logger.info("Removed {} deleted objects from storage", deleted.size)
        return deleted.size
    }

    private fun requireOwnedFile(
        fileId: UUID,
        actor: Actor,
    ): StoredFile {
        val file =
            fileRepository.findById(fileId)
                ?: throw ApiException(FileError.FILE_NOT_FOUND, mapOf("fileId" to fileId.toString()))

        if (!file.isOwnedBy(actor.id)) {
            throw ApiException(FileError.FILE_NOT_FOUND, mapOf("fileId" to fileId.toString()))
        }
        return file
    }

    private fun objectKeyFor(
        scope: String,
        fileId: UUID,
        originalName: String,
    ): String {
        val extension = originalName.substringAfterLast('.', "").lowercase().filter { it.isLetterOrDigit() }
        val suffix = if (extension.isEmpty()) "" else ".$extension"
        return "${scope.lowercase()}/$fileId$suffix"
    }
}
