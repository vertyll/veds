@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.project.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class TranslationTest {
    private val pl = LanguageTag.of("pl")
    private val en = LanguageTag.of("en")

    @Test
    fun `accepts a single language`() {
        val category =
            ProjectCategory.create(
                projectId = Uuid.generateV7().toJavaUuid(),
                color = "#fff",
                translations = setOf(Translation(pl, "Blad")),
            )

        assertEquals(1, category.translations.size)
    }

    @Test
    fun `rejects an empty translation set`() {
        assertFailsWith<IllegalArgumentException> {
            ProjectStatus.create(
                projectId = Uuid.generateV7().toJavaUuid(),
                color = "#fff",
                translations = emptySet(),
            )
        }
    }

    @Test
    fun `rejects two translations for the same language`() {
        assertFailsWith<IllegalArgumentException> {
            ProjectCategory.create(
                projectId = Uuid.generateV7().toJavaUuid(),
                color = "#fff",
                translations = setOf(Translation(pl, "Blad"), Translation(pl, "Usterka")),
            )
        }
    }

    @Test
    fun `resolves the requested language when present`() {
        val category =
            ProjectCategory.create(
                projectId = Uuid.generateV7().toJavaUuid(),
                color = "#fff",
                translations = setOf(Translation(pl, "Blad"), Translation(en, "Bug")),
            )

        assertEquals("Blad", category.translationFor(pl).name)
        assertEquals("Bug", category.translationFor(en).name)
    }

    @Test
    fun `falls back to what the author wrote when the language is missing`() {
        val category =
            ProjectCategory.create(
                projectId = Uuid.generateV7().toJavaUuid(),
                color = "#fff",
                translations = setOf(Translation(pl, "Blad")),
            )

        val resolved = category.translationFor(en)

        assertEquals("Blad", resolved.name)
        assertEquals(pl, resolved.language)
    }

    @Test
    fun `rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { Translation(en, "   ") }
    }

    @Test
    fun `normalises and validates language tags`() {
        assertEquals("pl", LanguageTag.of(" PL ").value)
        assertEquals("de-at", LanguageTag.of("de-AT").value)
        assertFailsWith<IllegalArgumentException> { LanguageTag.of("polski!") }
    }
}
