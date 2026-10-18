package com.vertyll.veds.translation.infrastructure.web.controller

import com.vertyll.veds.shared.web.security.PublicEndpoint
import com.vertyll.veds.translation.application.command.CatalogueEntryCommand
import com.vertyll.veds.translation.application.command.RegisterCatalogueCommand
import com.vertyll.veds.translation.application.port.inbound.command.TranslationCommandUseCase
import com.vertyll.veds.translation.infrastructure.web.dto.RegisterCatalogueRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/translations")
@Tag(name = "Translation registration", description = "Service-to-service key declaration")
@PublicEndpoint("service-to-service registration inside the cluster; the gateway routes no /internal path and NetworkPolicy keeps it there")
internal class TranslationRegistrationController(
    private val commands: TranslationCommandUseCase,
) {
    @PostMapping("/catalogue")
    @Operation(summary = "Register the calling service's translation keys")
    fun register(
        @Valid @RequestBody
        request: RegisterCatalogueRequest,
    ): ResponseEntity<Int> {
        val applied =
            commands.registerCatalogue(
                RegisterCatalogueCommand(
                    sourceService = request.sourceService,
                    entries =
                        request.entries.map {
                            CatalogueEntryCommand(
                                key = it.key,
                                description = it.description,
                                defaultValues = it.defaultValues,
                            )
                        },
                ),
            )
        return ResponseEntity.ok(applied)
    }
}
