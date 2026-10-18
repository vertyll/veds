package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectCategoryCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectCategoryQueryUseCase
import com.vertyll.veds.project.infrastructure.web.LanguageHeader
import com.vertyll.veds.project.infrastructure.web.dto.CreateProjectCategoryRequest
import com.vertyll.veds.project.infrastructure.web.dto.UpdateProjectCategoryRequest
import com.vertyll.veds.project.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.shared.web.security.AuthorizedInUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects/{projectId}/categories")
@Tag(name = "Project categories", description = "Category management within a project")
@AuthorizedInUseCase("ProjectAuthorizationService — a category is reachable only through its project")
internal class ProjectCategoryController(
    private val categoryServiceCommands: ProjectCategoryCommandUseCase,
    private val categoryServiceQueries: ProjectCategoryQueryUseCase,
) {
    @GetMapping
    @Operation(summary = "List categories of a project")
    fun getCategories(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<List<ProjectCategoryResponse>> {
        val categories =
            categoryServiceQueries.getCategories(
                projectId = projectId,
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ResponseEntity.ok(categories)
    }

    @PostMapping
    @Operation(summary = "Create a category in a project")
    fun createCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @Valid @RequestBody
        request: CreateProjectCategoryRequest,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<ProjectCategoryResponse> {
        val category =
            categoryServiceCommands.createCategory(
                projectId = projectId,
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(category)
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a category")
    @Suppress("LongParameterList")
    fun updateCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @PathVariable categoryId: UUID,
        @Valid @RequestBody
        request: UpdateProjectCategoryRequest,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ProjectCategoryResponse> {
        val category =
            categoryServiceCommands.updateCategory(
                projectId = projectId,
                categoryId = categoryId,
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ResponseEntity.ok(category)
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete a category")
    fun deleteCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @PathVariable categoryId: UUID,
    ): ResponseEntity<Any> {
        categoryServiceCommands.deleteCategory(projectId, categoryId, CurrentUser.idOf(jwt))
        return ResponseEntity.ok().build()
    }
}
