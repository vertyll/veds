package com.vertyll.veds.translation.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TranslationKeyTest {
    private fun key() = TranslationKey(key = "project.not_found", sourceService = PROJECT_SERVICE)

    @Test
    fun `the owning service may re-declare its own key`() {
        val updated = key().redeclaredBy(PROJECT_SERVICE, "Raised when a project id is unknown")

        assertEquals("Raised when a project id is unknown", updated.description)
    }

    @Test
    fun `another service may not claim it`() {
        assertFailsWith<IllegalStateException> { key().redeclaredBy("task-service", null) }
    }

    @Test
    fun `re-declaring without a description keeps the existing one`() {
        val described = key().redeclaredBy(PROJECT_SERVICE, "Original")
        val again = described.redeclaredBy(PROJECT_SERVICE, null)

        assertEquals("Original", again.description)
    }

    @Test
    fun `rejects a key that is not dotted lower case`() {
        assertFailsWith<IllegalArgumentException> { TranslationKey("ProjectNotFound", PROJECT_SERVICE) }
        assertFailsWith<IllegalArgumentException> { TranslationKey("project", PROJECT_SERVICE) }
        assertFailsWith<IllegalArgumentException> { TranslationKey("", PROJECT_SERVICE) }
    }
}

private const val PROJECT_SERVICE = "project-service"
