package com.vertyll.veds.translation.infrastructure.web.controller

import com.vertyll.veds.translation.application.command.ImportTranslationsCommand
import com.vertyll.veds.translation.application.dto.ImportReportResponse
import com.vertyll.veds.translation.application.port.inbound.command.TranslationCommandUseCase
import com.vertyll.veds.translation.application.port.inbound.query.TranslationExportUseCase
import com.vertyll.veds.translation.application.port.inbound.query.TranslationQueryUseCase
import com.vertyll.veds.translation.infrastructure.spreadsheet.TranslationSpreadsheet
import com.vertyll.veds.translation.infrastructure.web.LanguageHeader
import com.vertyll.veds.translation.infrastructure.web.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/admin/translations")
@Tag(name = "Translation administration", description = "Spreadsheet import and export")
@PreAuthorize("@authz.has('TRANSLATIONS_VIEW')")
internal class TranslationSpreadsheetController(
    private val exports: TranslationExportUseCase,
    private val queries: TranslationQueryUseCase,
    private val commands: TranslationCommandUseCase,
    private val spreadsheet: TranslationSpreadsheet,
) {
    private companion object {
        private const val FILE_NAME = "translations.xlsx"
    }

    @GetMapping("/export")
    @Operation(summary = "Download the whole catalogue as a spreadsheet")
    fun export(
        @RequestHeader(LanguageHeader.NAME) language: String,
    ): ResponseEntity<ByteArray> {
        val languages = queries.languages().map { it.tag }
        val headers = exports.exportHeaders(CurrentUser.languageOf(language))
        val file = spreadsheet.write(headers, languages, exports.exportRows())

        return ResponseEntity
            .ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition
                    .attachment()
                    .filename(FILE_NAME)
                    .build()
                    .toString(),
            ).contentType(MediaType.parseMediaType(TranslationSpreadsheet.CONTENT_TYPE))
            .body(file)
    }

    @PreAuthorize("@authz.has('TRANSLATIONS_EDIT')")
    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Apply translations from a spreadsheet")
    fun import(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ImportReportResponse> {
        val languages = queries.languages().map { it.tag }
        val entries = file.inputStream.use { spreadsheet.read(it, languages) }

        val report =
            commands.import(
                ImportTranslationsCommand(entries = entries, importedBy = CurrentUser.idOf(jwt)),
            )
        return ResponseEntity.ok(report)
    }
}
