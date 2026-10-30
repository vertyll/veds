package com.vertyll.veds.translation.application.service.query

import com.vertyll.veds.translation.application.ENGLISH
import com.vertyll.veds.translation.application.InMemoryKeyRepository
import com.vertyll.veds.translation.application.InMemoryLanguageRepository
import com.vertyll.veds.translation.application.InMemoryValueRepository
import com.vertyll.veds.translation.application.POLISH
import com.vertyll.veds.translation.domain.model.Language
import com.vertyll.veds.translation.domain.model.TranslationValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class TranslationExportServiceTest {
    private val keys = InMemoryKeyRepository()
    private val values = InMemoryValueRepository()
    private val languages = InMemoryLanguageRepository()

    private val service = TranslationExportService(keys, values, languages)

    @Test
    fun `the headers read in the language asked for, then name each language`() {
        languages.given(
            Language(tag = ENGLISH, displayName = "English", isDefault = true),
            Language(tag = POLISH, displayName = "Polski"),
        )
        values.given(
            TranslationValue(key = "translation.export.column.key", language = POLISH, defaultValue = "Klucz"),
            TranslationValue(key = "translation.export.column.source_service", language = POLISH, defaultValue = "Usługa"),
            TranslationValue(key = "translation.export.column.description", language = POLISH, overrideValue = "Opis"),
        )

        val headers = service.exportHeaders(POLISH)

        assertEquals(listOf("Klucz", "Usługa", "Opis", ENGLISH.value, POLISH.value), headers)
    }
}
