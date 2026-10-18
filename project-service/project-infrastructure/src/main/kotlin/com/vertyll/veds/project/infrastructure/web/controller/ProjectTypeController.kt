package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectTypeQueryUseCase
import com.vertyll.veds.project.infrastructure.web.LanguageHeader
import com.vertyll.veds.project.infrastructure.web.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/project-types")
@Tag(name = "Project types", description = "Project type reference data")
@PreAuthorize("isAuthenticated()")
internal class ProjectTypeController(
    private val typeServiceQueries: ProjectTypeQueryUseCase,
) {
    @GetMapping
    @Operation(summary = "List all active project types")
    fun getTypes(
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<List<ProjectTypeResponse>> {
        val types = typeServiceQueries.getAllTypes(CurrentUser.languageOf(acceptLanguage))
        return ResponseEntity.ok(types)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project type by id")
    fun getType(
        @PathVariable id: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<ProjectTypeResponse> {
        val type = typeServiceQueries.getTypeById(id, CurrentUser.languageOf(acceptLanguage))
        return ResponseEntity.ok(type)
    }
}
