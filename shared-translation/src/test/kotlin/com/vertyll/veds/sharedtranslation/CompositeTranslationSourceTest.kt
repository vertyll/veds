package com.vertyll.veds.sharedtranslation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompositeTranslationSourceTest {
    private val shipped = mapOf("pl" to mapOf("mail.subject" to "Powiadomienie"))

    @Test
    fun `prefers a live snapshot over the shipped default`() {
        val source =
            CompositeTranslationSource(
                snapshots = { mapOf("pl" to TranslationSnapshot("pl", "7", mapOf("mail.subject" to "Nowe powiadomienie"))) },
                fallbackDefaults = shipped,
            )

        assertEquals("Nowe powiadomienie", source.patternFor("mail.subject", "pl"))
    }

    /**
     * Keeps mail going out with sensible text when the catalogue service was unreachable at
     * start-up. It is not a per-key fallback: a key absent from both still renders as the key.
     */
    @Test
    fun `falls back to the shipped default when no snapshot was loaded`() {
        val source = CompositeTranslationSource(snapshots = { emptyMap() }, fallbackDefaults = shipped)

        assertEquals("Powiadomienie", source.patternFor("mail.subject", "pl"))
        assertNull(source.patternFor("mail.absent", "pl"))
    }

    @Test
    fun `matches the language case-insensitively`() {
        val source = CompositeTranslationSource(snapshots = { emptyMap() }, fallbackDefaults = shipped)

        assertEquals("Powiadomienie", source.patternFor("mail.subject", "PL"))
    }
}
