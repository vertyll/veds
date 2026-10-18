@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.file.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class StoredFileTest {
    private val owner = Uuid.generateV7().toJavaUuid()

    private fun pending() =
        StoredFile.pending(
            objectKey = "task_attachment/abc.pdf",
            originalName = "abc.pdf",
            contentType = "application/pdf",
            declaredSizeBytes = 1024,
            ownerId = owner,
            scope = FileScope.TASK_ATTACHMENT,
        )

    @Test
    fun `a new upload is pending and not yet available`() {
        val file = pending()

        assertEquals(UploadStatus.PENDING, file.status)
        assertFalse(file.isAvailable)
    }

    @Test
    fun `confirming replaces the declared size with the stored one`() {
        val confirmed = pending().confirmed(actualSizeBytes = 4096)

        assertEquals(UploadStatus.CONFIRMED, confirmed.status)
        assertEquals(4096, confirmed.sizeBytes)
        assertTrue(confirmed.isAvailable)
    }

    @Test
    fun `confirming twice is refused`() {
        val confirmed = pending().confirmed(1024)

        assertFailsWith<IllegalStateException> { confirmed.confirmed(1024) }
    }

    @Test
    fun `an empty object is not a completed upload`() {
        assertFailsWith<IllegalArgumentException> { pending().confirmed(actualSizeBytes = 0) }
    }

    @Test
    fun `deleting keeps the record and its key`() {
        val deleted = pending().deleted()

        assertEquals(UploadStatus.DELETED, deleted.status)
        assertEquals("task_attachment/abc.pdf", deleted.objectKey)
        assertFalse(deleted.isAvailable)
    }

    @Test
    fun `rejects a non-positive declared size`() {
        assertFailsWith<IllegalArgumentException> {
            StoredFile.pending("k", "n", "text/plain", 0, owner, FileScope.TASK_ATTACHMENT)
        }
    }

    @Test
    fun `recognises its owner`() {
        assertTrue(pending().isOwnedBy(owner))
        assertFalse(pending().isOwnedBy(Uuid.generateV7().toJavaUuid()))
    }
}
