@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.file.application.service.command

import com.vertyll.veds.file.application.command.ConfirmUploadCommand
import com.vertyll.veds.file.application.command.RequestUploadCommand
import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.application.dto.PresignedUrl
import com.vertyll.veds.file.application.port.outbound.FileEventPublisherPort
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
import com.vertyll.veds.file.application.port.outbound.UseCaseLogger
import com.vertyll.veds.file.domain.error.FileError
import com.vertyll.veds.file.domain.model.FileScope
import com.vertyll.veds.file.domain.model.StoredFile
import com.vertyll.veds.file.domain.model.UploadStatus
import com.vertyll.veds.file.domain.repository.StoredFileRepository
import com.vertyll.veds.sharederror.ApiException
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class FileCommandServiceTest {
    private val actor = Actor(Uuid.generateV7().toJavaUuid(), "owner@example.com")
    private val files = mutableMapOf<UUID, StoredFile>()
    private val confirmedEvents = mutableListOf<UUID>()
    private var storedSize: Long? = 1_000

    private val repository =
        object : StoredFileRepository {
            override fun save(file: StoredFile): StoredFile = file.also { files[it.id] = it }

            override fun saveAll(files: Collection<StoredFile>): List<StoredFile> = files.map(::save)

            override fun findById(id: UUID): StoredFile? = files[id]

            override fun findAllByIds(ids: Collection<UUID>): List<StoredFile> = ids.mapNotNull { files[it] }

            override fun findAllByScopeId(scopeId: UUID): List<StoredFile> = emptyList()

            override fun findAbandoned(
                status: UploadStatus,
                createdBefore: Instant,
            ): List<StoredFile> = emptyList()

            override fun findAllDeleted(): List<StoredFile> = emptyList()

            override fun delete(id: UUID) {
                files.remove(id)
            }
        }

    private val storage =
        object : ObjectStoragePort {
            override fun presignUpload(
                objectKey: String,
                contentType: String,
            ): PresignedUrl = PresignedUrl("https://store.example/$objectKey", Instant.now().plusSeconds(900))

            override fun presignDownload(
                objectKey: String,
                originalName: String,
                contentType: String,
            ): PresignedUrl = PresignedUrl("https://store.example/$objectKey", Instant.now().plusSeconds(300))

            override fun sizeOf(objectKey: String): Long? = storedSize

            override fun delete(objectKey: String) {
                deleted += objectKey
            }
        }

    private val deleted = mutableListOf<String>()

    private val publisher =
        object : FileEventPublisherPort {
            override fun publishFileConfirmed(
                fileId: UUID,
                scope: FileScope,
                scopeId: UUID?,
                ownerId: UUID,
            ) {
                confirmedEvents += fileId
            }

            override fun publishFileDeleted(
                fileId: UUID,
                scope: FileScope,
                scopeId: UUID?,
            ) = Unit
        }

    private val silentLogger =
        object : UseCaseLogger {
            override fun debug(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun info(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun warn(
                message: String,
                vararg args: Any?,
            ) = Unit

            override fun error(
                message: String,
                vararg args: Any?,
            ) = Unit
        }

    private val service = FileCommandService(repository, storage, publisher, silentLogger)

    private fun requestAvatar(
        contentType: String = "image/png",
        declaredSize: Long = 1_000,
    ) = service.requestUpload(
        RequestUploadCommand("avatar.png", contentType, declaredSize, FileScope.USER_AVATAR, null),
        actor,
    )

    @Test
    fun `refuses a content type the scope does not allow`() {
        val failure = assertFailsWith<ApiException> { requestAvatar(contentType = "application/x-msdownload") }

        assertEquals(FileError.CONTENT_TYPE_NOT_ALLOWED, failure.error)
    }

    @Test
    fun `refuses a declared size beyond the scope limit`() {
        val failure = assertFailsWith<ApiException> { requestAvatar(declaredSize = FileScope.USER_AVATAR.maxSizeBytes + 1) }

        assertEquals(FileError.FILE_TOO_LARGE, failure.error)
    }

    @Test
    fun `hands out a ticket and records the file as pending`() {
        val ticket = requestAvatar()

        assertTrue(ticket.uploadUrl.isNotBlank())
        assertEquals(UploadStatus.PENDING, files.getValue(ticket.fileId).status)
        assertTrue(confirmedEvents.isEmpty(), "nothing is announced before the bytes exist")
    }

    @Test
    fun `records the size the store reports, not the one the client declared`() {
        val ticket = requestAvatar(declaredSize = 1_000)
        storedSize = 4_242

        service.confirmUpload(ConfirmUploadCommand(ticket.fileId), actor)

        assertEquals(4_242, files.getValue(ticket.fileId).sizeBytes)
    }

    @Test
    fun `an upload larger than its scope allows is refused and the object removed`() {
        val ticket = requestAvatar(declaredSize = 1_000)
        storedSize = FileScope.USER_AVATAR.maxSizeBytes + 1

        val failure = assertFailsWith<ApiException> { service.confirmUpload(ConfirmUploadCommand(ticket.fileId), actor) }

        assertEquals(FileError.FILE_TOO_LARGE, failure.error)
        assertEquals(listOf(files.getValue(ticket.fileId).objectKey), deleted)
        assertEquals(UploadStatus.PENDING, files.getValue(ticket.fileId).status, "the record is not confirmed")
    }

    @Test
    fun `refuses to confirm a file the store does not hold`() {
        val ticket = requestAvatar()
        storedSize = null

        val failure = assertFailsWith<ApiException> { service.confirmUpload(ConfirmUploadCommand(ticket.fileId), actor) }

        assertEquals(FileError.OBJECT_MISSING_IN_STORAGE, failure.error)
        assertTrue(confirmedEvents.isEmpty(), "a file that does not exist is never announced")
    }

    @Test
    fun `announces a confirmed upload exactly once`() {
        val ticket = requestAvatar()

        service.confirmUpload(ConfirmUploadCommand(ticket.fileId), actor)
        val second = assertFailsWith<ApiException> { service.confirmUpload(ConfirmUploadCommand(ticket.fileId), actor) }

        assertEquals(FileError.UPLOAD_NOT_PENDING, second.error)
        assertEquals(listOf(ticket.fileId), confirmedEvents)
    }
}
