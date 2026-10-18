package com.vertyll.veds.translation.infrastructure.spreadsheet

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.translation.application.command.ImportedTranslationCommand
import com.vertyll.veds.translation.application.dto.ExportRowResponse
import com.vertyll.veds.translation.domain.error.TranslationError
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Component
internal class TranslationSpreadsheet {
    companion object {
        const val CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        private const val SHEET_NAME = "translations"
        private const val KEY_COLUMN = 0
        private const val FIRST_LANGUAGE_COLUMN = 3
        private const val ROW_WINDOW = 200
    }

    fun write(
        headers: List<String>,
        languages: List<String>,
        rows: List<ExportRowResponse>,
    ): ByteArray {
        SXSSFWorkbook(ROW_WINDOW).use { workbook ->
            val sheet = workbook.createSheet(SHEET_NAME)

            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header -> headerRow.createCell(index).setCellValue(header) }

            rows.forEachIndexed { rowIndex, row ->
                val sheetRow = sheet.createRow(rowIndex + 1)
                sheetRow.createCell(KEY_COLUMN).setCellValue(row.key)
                sheetRow.createCell(1).setCellValue(row.sourceService)
                sheetRow.createCell(2).setCellValue(row.description ?: "")
                languages.forEachIndexed { languageIndex, language ->
                    sheetRow
                        .createCell(FIRST_LANGUAGE_COLUMN + languageIndex)
                        .setCellValue(row.values[language] ?: "")
                }
            }

            return ByteArrayOutputStream().use { out ->
                workbook.write(out)
                workbook.close()
                out.toByteArray()
            }
        }
    }

    fun read(
        input: InputStream,
        languages: List<String>,
    ): List<ImportedTranslationCommand> {
        WorkbookFactory.create(input).use { workbook ->
            if (workbook.numberOfSheets == 0) {
                throw ApiException(TranslationError.IMPORT_MALFORMED)
            }
            val sheet = workbook.getSheetAt(0)
            val commands = mutableListOf<ImportedTranslationCommand>()

            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex)
                val key = row?.stringAt(KEY_COLUMN)?.trim().orEmpty()
                if (row == null || key.isEmpty()) continue

                languages.forEachIndexed { languageIndex, language ->
                    val value = row.stringAt(FIRST_LANGUAGE_COLUMN + languageIndex)?.trim()
                    if (!value.isNullOrEmpty()) {
                        commands += ImportedTranslationCommand(key = key, language = language, value = value)
                    }
                }
            }

            return commands
        }
    }

    private fun Row.stringAt(index: Int): String? {
        val cell = getCell(index) ?: return null
        return runCatching { cell.stringCellValue }
            .getOrElse { runCatching { cell.numericCellValue.toLong().toString() }.getOrNull() }
    }
}
