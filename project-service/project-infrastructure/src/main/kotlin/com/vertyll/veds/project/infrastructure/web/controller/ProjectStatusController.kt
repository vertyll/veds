package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectStatusCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectStatusQueryUseCase
import com.vertyll.veds.project.infrastructure.web.LanguageHeader
import com.vertyll.veds.project.infrastructure.web.dto.CreateProjectStatusRequest
import com.vertyll.veds.project.infrastructure.web.dto.UpdateProjectStatusRequest
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
@RequestMapping("/projects/{projectId}/statuses")
@Tag(name = "Project statuses", description = "Workflow status management within a project")
@AuthorizedInUseCase("ProjectAuthorizationService — a status is reachable only through its project")
internal class ProjectStatusController(
    private val statusServiceCommands: ProjectStatusCommandUseCase,
    private val statusServiceQueries: ProjectStatusQueryUseCase,
) {
    @GetMapping
    @Operation(summary = "List statuses of a project")
    fun getStatuses(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<List<ProjectStatusResponse>> {
        val statuses =
            statusServiceQueries.getStatuses(
                projectId = projectId,
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ResponseEntity.ok(statuses)
    }

    @PostMapping
    @Operation(summary = "Create a status in a project")
    fun createStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @Valid @RequestBody
        request: CreateProjectStatusRequest,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<ProjectStatusResponse> {
        val status =
            statusServiceCommands.createStatus(
                projectId = projectId,
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(status)
    }

    @PutMapping("/{statusId}")
    @Operation(summary = "Update a status")
    @Suppress("LongParameterList")
    fun updateStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @PathVariable statusId: UUID,
        @Valid @RequestBody
        request: UpdateProjectStatusRequest,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<ProjectStatusResponse> {
        val status =
            statusServiceCommands.updateStatus(
                projectId = projectId,
                statusId = statusId,
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ResponseEntity.ok(status)
    }

    @DeleteMapping("/{statusId}")
    @Operation(summary = "Delete a status")
    fun deleteStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @PathVariable statusId: UUID,
    ): ResponseEntity<Any> {
        statusServiceCommands.deleteStatus(projectId, statusId, CurrentUser.idOf(jwt))
        return ResponseEntity.ok().build()
    }
}
