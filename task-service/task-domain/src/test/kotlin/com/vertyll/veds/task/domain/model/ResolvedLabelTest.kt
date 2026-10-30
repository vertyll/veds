package com.vertyll.veds.task.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ResolvedLabelTest {
    @Test
    fun `a label is named in the language asked for`() {
        val label = mapOf("en" to "Bug", "pl" to "Błąd").resolveLabel(LanguageTag("pl"))

        assertEquals(ResolvedLabel(name = "Błąd", language = "pl"), label)
    }

    @Test
    fun `a label missing that language falls back to the first it has, and says which`() {
        val label = mapOf("pl" to "Błąd", "en" to "Bug").resolveLabel(LanguageTag("de"))

        assertEquals(ResolvedLabel(name = "Bug", language = "en"), label)
    }

    @Test
    fun `a label with no names at all is a broken projection`() {
        assertFailsWith<IllegalStateException> { emptyMap<String, String>().resolveLabel(LanguageTag("pl")) }
    }
}
