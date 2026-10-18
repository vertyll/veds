package com.vertyll.veds.file.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileScopeTest {
    @Test
    fun `avatars accept images only`() {
        assertTrue(FileScope.USER_AVATAR.permits("image/png"))
        assertFalse(FileScope.USER_AVATAR.permits("application/pdf"))
    }

    @Test
    fun `content type matching ignores case`() {
        assertTrue(FileScope.PROJECT_ICON.permits("IMAGE/PNG"))
    }

    @Test
    fun `attachments accept any type`() {
        assertTrue(FileScope.TASK_ATTACHMENT.permits("application/x-7z-compressed"))
    }

    @Test
    fun `size is capped even where the type is not`() {
        assertTrue(FileScope.TASK_ATTACHMENT.permitsSize(1))
        assertTrue(FileScope.TASK_ATTACHMENT.permitsSize(FileScope.TASK_ATTACHMENT.maxSizeBytes))
        assertFalse(FileScope.TASK_ATTACHMENT.permitsSize(FileScope.TASK_ATTACHMENT.maxSizeBytes + 1))
    }

    @Test
    fun `an empty file is never permitted`() {
        FileScope.entries.forEach { scope ->
            assertFalse(scope.permitsSize(0), "$scope should reject an empty file")
        }
    }
}
