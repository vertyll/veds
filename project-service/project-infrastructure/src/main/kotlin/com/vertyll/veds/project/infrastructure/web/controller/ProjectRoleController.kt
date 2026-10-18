package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectRoleResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectRoleQueryUseCase
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
@RequestMapping("/project-roles")
@Tag(name = "Project roles", description = "Project role and permission reference data")
@PreAuthorize("isAuthenticated()")
internal class ProjectRoleController(
    private val roleServiceQueries: ProjectRoleQueryUseCase,
) {
    @GetMapping
    @Operation(summary = "List all active project roles")
    fun getRoles(
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<List<ProjectRoleResponse>> {
        val roles = roleServiceQueries.getAllRoles(CurrentUser.languageOf(acceptLanguage))
        return ResponseEntity.ok(roles)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a project role by id")
    fun getRole(
        @PathVariable id: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<ProjectRoleResponse> {
        val role = roleServiceQueries.getRoleById(id, CurrentUser.languageOf(acceptLanguage))
        return ResponseEntity.ok(role)
    }
}
