package com.vertyll.veds.translation.infrastructure.web.controller

import com.vertyll.veds.shared.web.security.PublicEndpoint
import com.vertyll.veds.translation.application.dto.LanguageResponse
import com.vertyll.veds.translation.application.dto.TranslationSnapshotResponse
import com.vertyll.veds.translation.application.port.inbound.query.TranslationQueryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/translations")
@Tag(name = "Translations", description = "Public translation catalogue")
@PublicEndpoint("the sign-in screen needs its catalogue before anyone has a token")
internal class TranslationController(
    private val queries: TranslationQueryUseCase,
) {
    private companion object {
        private const val CACHE_MINUTES = 5L
    }

    @GetMapping("/{language}")
    @Operation(summary = "Get every effective translation for one language")
    fun snapshot(
        @PathVariable language: String,
    ): ResponseEntity<TranslationSnapshotResponse> {
        val snapshot = queries.snapshot(language)
        val body = ResponseEntity.ok(snapshot).body

        return ResponseEntity
            .ok()
            .eTag("\"${snapshot.version}\"")
            .cacheControl(CacheControl.maxAge(CACHE_MINUTES, TimeUnit.MINUTES).cachePublic())
            .body(body)
    }

    @GetMapping("/languages")
    @Operation(summary = "List the languages the application offers")
    fun languages(): ResponseEntity<List<LanguageResponse>> = ResponseEntity.ok(queries.languages())
}
