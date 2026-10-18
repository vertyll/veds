package com.vertyll.veds.translation.infrastructure.web.controller

import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.translation.application.command.ClearOverrideCommand
import com.vertyll.veds.translation.application.command.OverrideTranslationCommand
import com.vertyll.veds.translation.application.dto.PagedResponse
import com.vertyll.veds.translation.application.dto.TranslationKeyDetailsResponse
import com.vertyll.veds.translation.application.dto.TranslationValueResponse
import com.vertyll.veds.translation.application.port.inbound.command.TranslationCommandUseCase
import com.vertyll.veds.translation.application.port.inbound.query.TranslationQueryUseCase
import com.vertyll.veds.translation.infrastructure.web.dto.OverrideTranslationRequest
import com.vertyll.veds.translation.infrastructure.web.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/translations")
@Tag(name = "Translation administration", description = "Editing the translation catalogue")
@PreAuthorize("@authz.has('TRANSLATIONS_VIEW')")
internal class TranslationAdminController(
    private val commands: TranslationCommandUseCase,
    private val queries: TranslationQueryUseCase,
) {
    private companion object {
        private const val DEFAULT_PAGE_SIZE = "50"
    }

    @GetMapping("/keys")
    @Operation(summary = "List keys with their values in every language")
    @Suppress("LongParameterList")
    fun searchKeys(
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) sourceService: String?,
        @RequestParam(defaultValue = "false") onlyMissing: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) size: Int,
    ): ResponseEntity<PagedResponse<TranslationKeyDetailsResponse>> {
        val keys = queries.searchKeys(searchTerm, sourceService, onlyMissing, page, size)
        return ResponseEntity.ok(keys)
    }

    @GetMapping("/keys/{key}")
    @Operation(summary = "Get one key with every language")
    fun keyDetails(
        @PathVariable key: String,
    ): ResponseEntity<TranslationKeyDetailsResponse> = ResponseEntity.ok(queries.keyDetails(key))

    @PreAuthorize("@authz.has('TRANSLATIONS_EDIT')")
    @PutMapping("/keys/{key}/languages/{language}")
    @Operation(summary = "Set the translation of one key in one language")
    fun override(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable key: String,
        @PathVariable language: String,
        @Valid @RequestBody
        request: OverrideTranslationRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<TranslationValueResponse> {
        val value =
            commands.override(
                command = OverrideTranslationCommand(key = key, language = language, value = request.value),
                editor = CurrentUser.idOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ResponseEntity.ok(value)
    }

    @PreAuthorize("@authz.has('TRANSLATIONS_EDIT')")
    @DeleteMapping("/keys/{key}/languages/{language}")
    @Operation(summary = "Drop an override and fall back to the shipped default")
    fun clearOverride(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable key: String,
        @PathVariable language: String,
    ): ResponseEntity<TranslationValueResponse> {
        val value =
            commands.clearOverride(
                command = ClearOverrideCommand(key = key, language = language),
                editor = CurrentUser.idOf(jwt),
            )
        return ResponseEntity.ok(value)
    }
}
