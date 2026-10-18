package com.vertyll.veds.translation.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LanguageTagTest {
    @Test
    fun `normalises case and whitespace`() {
        assertEquals("pl", LanguageTag.of("  PL ").value)
        assertEquals("de-at", LanguageTag.of("de-AT").value)
    }

    @Test
    fun `rejects anything that is not a tag`() {
        assertFailsWith<IllegalArgumentException> { LanguageTag.of("polski!") }
        assertFailsWith<IllegalArgumentException> { LanguageTag.of("") }
    }

    @Test
    fun `parse takes the first tag of an Accept-Language style list`() {
        assertEquals("en", LanguageTag.parse("en,pl;q=0.8")?.value)
    }

    @Test
    fun `parse returns null rather than throwing on rubbish`() {
        assertNull(LanguageTag.parse("???"))
        assertNull(LanguageTag.parse(null))
    }
}
