@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.translation.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class TranslationValueTest {
    private val editor = Uuid.generateV7().toJavaUuid()
    private val pl = LanguageTag.of("pl")

    private fun seeded() = TranslationValue.seeded("project.not_found", pl, NIE_ZNALEZIONO_PROJEKTU)

    @Test
    fun `a seeded value is effective until somebody overrides it`() {
        val value = seeded()

        assertEquals(NIE_ZNALEZIONO_PROJEKTU, value.effectiveValue)
        assertFalse(value.isOverridden)
    }

    @Test
    fun `an override wins over the shipped default`() {
        val overridden = seeded().overriddenBy(editor, TAKI_PROJEKT_NIE_ISTNIEJE)

        assertEquals(TAKI_PROJEKT_NIE_ISTNIEJE, overridden.effectiveValue)
        assertTrue(overridden.isOverridden)
        assertEquals(editor, overridden.updatedBy)
    }

    @Test
    fun `re-seeding leaves the override alone`() {
        val overridden = seeded().overriddenBy(editor, TAKI_PROJEKT_NIE_ISTNIEJE)
        val reseeded = overridden.withSeededDefault("Project not found.")

        assertEquals(TAKI_PROJEKT_NIE_ISTNIEJE, reseeded.effectiveValue)
        assertEquals("Project not found.", reseeded.defaultValue)
    }

    @Test
    fun `re-seeding the same value returns the same instance`() {
        val value = seeded()

        assertSame(value, value.withSeededDefault(NIE_ZNALEZIONO_PROJEKTU))
    }

    @Test
    fun `clearing an override falls back to the shipped default`() {
        val reverted = seeded().overriddenBy(editor, "Coś innego.").overrideCleared(editor)

        assertEquals(NIE_ZNALEZIONO_PROJEKTU, reverted.effectiveValue)
        assertFalse(reverted.isOverridden)
    }

    @Test
    fun `a value with neither column has no effective text`() {
        val empty = TranslationValue(key = "project.not_found", language = pl)

        assertNull(empty.effectiveValue)
    }

    @Test
    fun `an override may not be blank`() {
        assertFailsWith<IllegalArgumentException> { seeded().overriddenBy(editor, "   ") }
    }
}

private const val NIE_ZNALEZIONO_PROJEKTU = "Nie znaleziono projektu."
private const val TAKI_PROJEKT_NIE_ISTNIEJE = "Taki projekt nie istnieje."
