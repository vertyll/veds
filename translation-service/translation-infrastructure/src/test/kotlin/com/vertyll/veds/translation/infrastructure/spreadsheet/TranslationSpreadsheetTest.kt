package com.vertyll.veds.translation.infrastructure.spreadsheet

import com.vertyll.veds.translation.application.command.ImportedTranslationCommand
import com.vertyll.veds.translation.application.dto.ExportRowResponse
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

internal class TranslationSpreadsheetTest {
    private val spreadsheet = TranslationSpreadsheet()
    private val languages = listOf("pl", "en")
    private val headers = listOf("Klucz", "Usługa", "Opis", "pl", "en")

    private fun row(
        key: String,
        description: String? = null,
        values: Map<String, String>,
    ) = ExportRowResponse(key, "task-service", description, values, emptyMap())

    @Test
    fun `what was exported reads back as the same translations`() {
        val written =
            spreadsheet.write(
                headers,
                languages,
                listOf(row("task.not_found", "Shown on a 404", mapOf("pl" to "Nie znaleziono", "en" to "Not found"))),
            )

        val imported = spreadsheet.read(ByteArrayInputStream(written), languages)

        assertEquals(
            listOf(
                ImportedTranslationCommand("task.not_found", "pl", "Nie znaleziono"),
                ImportedTranslationCommand("task.not_found", "en", "Not found"),
            ),
            imported,
        )
    }

    @Test
    fun `a language with no value and a key with no description export as empty cells, which import nothing`() {
        val written = spreadsheet.write(headers, languages, listOf(row("task.not_found", values = mapOf("pl" to "Nie znaleziono"))))

        val imported = spreadsheet.read(ByteArrayInputStream(written), languages)

        assertEquals(listOf(ImportedTranslationCommand("task.not_found", "pl", "Nie znaleziono")), imported)
    }
}
