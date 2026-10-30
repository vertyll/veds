@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.file.application.service.query

import com.vertyll.veds.file.application.dto.Actor
import com.vertyll.veds.file.application.dto.PresignedUrl
import com.vertyll.veds.file.application.port.outbound.ObjectStoragePort
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class FileQueryServiceTest {
    private val owner = Actor(Uuid.generateV7().toJavaUuid(), "owner@example.com")
    private val stranger = Actor(Uuid.generateV7().toJavaUuid(), "stranger@example.com")
    private val files = mutableMapOf<UUID, StoredFile>()
    private val expiresAt = Instant.parse("2026-01-01T00:05:00Z")

    private val repository =
        object : StoredFileRepository {
            override fun save(file: StoredFile): StoredFile = file.also { files[it.id] = it }

            override fun saveAll(files: Collection<StoredFile>): List<StoredFile> = files.map(::save)

            override fun findById(id: UUID): StoredFile? = files[id]

            override fun findAllByIds(ids: Collection<UUID>): List<StoredFile> = ids.mapNotNull { files[it] }

            override fun findAllByScopeId(scopeId: UUID): List<StoredFile> = files.values.filter { it.scopeId == scopeId }

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
            ): PresignedUrl = error("a read presigns no upload")

            override fun presignDownload(
                objectKey: String,
                originalName: String,
                contentType: String,
            ): PresignedUrl = PresignedUrl("https://store.example/$objectKey", expiresAt)

            override fun sizeOf(objectKey: String): Long? = null

            override fun delete(objectKey: String) = Unit
        }

    private val service = FileQueryService(repository, storage)

    private fun given(
        ownerId: UUID = owner.id,
        scopeId: UUID? = null,
        confirmed: Boolean = true,
    ): StoredFile {
        val pending =
            StoredFile.pending(
                objectKey = "avatars/${UUID.randomUUID()}",
                originalName = "avatar.png",
                contentType = "image/png",
                declaredSizeBytes = 1_000,
                ownerId = ownerId,
                scope = FileScope.USER_AVATAR,
                scopeId = scopeId,
            )
        return repository.save(if (confirmed) pending.confirmed(1_000) else pending)
    }

    @Test
    fun `a download ticket names the file and the store's address`() {
        val file = given()

        val ticket = service.requestDownload(file.id, owner)

        assertEquals(file.id, ticket.fileId)
        assertEquals("https://store.example/${file.objectKey}", ticket.downloadUrl)
        assertEquals("avatar.png", ticket.originalName)
        assertEquals("image/png", ticket.contentType)
        assertEquals(expiresAt, ticket.expiresAt)
    }

    @Test
    fun `an upload that was never confirmed cannot be downloaded`() {
        val file = given(confirmed = false)

        val failure = assertFailsWith<ApiException> { service.requestDownload(file.id, owner) }

        assertEquals(FileError.FILE_NOT_AVAILABLE, failure.error)
    }

    @Test
    fun `a file that does not exist is not found`() {
        val failure = assertFailsWith<ApiException> { service.getFile(UUID.randomUUID(), owner) }

        assertEquals(FileError.FILE_NOT_FOUND, failure.error)
    }

    @Test
    fun `someone else's file looks like one that does not exist`() {
        val file = given()

        val failure = assertFailsWith<ApiException> { service.getFile(file.id, stranger) }

        assertEquals(FileError.FILE_NOT_FOUND, failure.error)
    }

    @Test
    fun `a scope lists only the caller's confirmed files`() {
        val scope = UUID.randomUUID()
        val listed = given(scopeId = scope)
        given(scopeId = scope, confirmed = false)
        given(ownerId = stranger.id, scopeId = scope)

        val found = service.listForScope(scope, owner)

        assertEquals(listOf(listed.id), found.map { it.id })
    }
}
