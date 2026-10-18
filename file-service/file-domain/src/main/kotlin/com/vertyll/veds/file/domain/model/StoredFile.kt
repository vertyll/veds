@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.file.domain.model

import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class StoredFile(
    val id: UUID = Uuid.generateV7().toJavaUuid(),
    val objectKey: String,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val status: UploadStatus = UploadStatus.PENDING,
    val ownerId: UUID,
    val scope: FileScope,
    val scopeId: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val confirmedAt: Instant? = null,
    val version: Long? = null,
) {
    init {
        require(objectKey.isNotBlank()) { "object key must not be blank" }
        require(originalName.isNotBlank()) { "original name must not be blank" }
        require(sizeBytes > 0) { "declared size must be positive" }
    }

    val isAvailable: Boolean
        get() = status == UploadStatus.CONFIRMED

    fun confirmed(actualSizeBytes: Long): StoredFile {
        check(status == UploadStatus.PENDING) { "only a pending upload can be confirmed" }
        require(actualSizeBytes > 0) { "stored object is empty" }
        return copy(status = UploadStatus.CONFIRMED, sizeBytes = actualSizeBytes, confirmedAt = Instant.now())
    }

    fun attachedTo(newScopeId: UUID): StoredFile = copy(scopeId = newScopeId)

    fun deleted(): StoredFile = copy(status = UploadStatus.DELETED)

    fun isOwnedBy(userId: UUID): Boolean = ownerId == userId

    companion object {
        @Suppress("LongParameterList")
        fun pending(
            objectKey: String,
            originalName: String,
            contentType: String,
            declaredSizeBytes: Long,
            ownerId: UUID,
            scope: FileScope,
            scopeId: UUID? = null,
        ): StoredFile =
            StoredFile(
                objectKey = objectKey,
                originalName = originalName,
                contentType = contentType,
                sizeBytes = declaredSizeBytes,
                ownerId = ownerId,
                scope = scope,
                scopeId = scopeId,
            )
    }
}
