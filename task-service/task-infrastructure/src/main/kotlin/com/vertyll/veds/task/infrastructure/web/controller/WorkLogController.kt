package com.vertyll.veds.task.infrastructure.web.controller

import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.shared.web.security.AuthorizedInUseCase
import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.dto.WorkLogEntryResponse
import com.vertyll.veds.task.application.dto.WorkLogPageResponse
import com.vertyll.veds.task.application.port.inbound.command.WorkLogCommandUseCase
import com.vertyll.veds.task.application.port.inbound.query.WorkLogQueryUseCase
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.WorkLogVisibility
import com.vertyll.veds.task.infrastructure.web.dto.LogWorkRequest
import com.vertyll.veds.task.infrastructure.web.dto.UpdateWorkLogRequest
import com.vertyll.veds.task.infrastructure.web.security.CurrentUser
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tasks")
@Tag(name = "Task work log", description = "Time logged against tasks")
@AuthorizedInUseCase("TaskAuthorizationService — a work log is reachable only through a task the caller may see")
internal class WorkLogController(
    private val workLogCommands: WorkLogCommandUseCase,
    private val workLogQueries: WorkLogQueryUseCase,
) {
    @GetMapping("/{taskId}/worklog")
    @Operation(summary = "List work logged against a task")
    fun getEntries(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "ALL") visibility: String,
    ): ResponseEntity<WorkLogPageResponse> {
        val chosen =
            WorkLogVisibility.fromString(visibility)
                ?: throw ApiException(TaskError.INVALID_WORK_LOG_VISIBILITY, mapOf("visibility" to visibility))
        val entries =
            workLogQueries.getEntries(taskId, CurrentUser.idOf(jwt), chosen, PageRequest(page = page, size = size))
        return ResponseEntity.ok(entries)
    }

    @PostMapping("/{taskId}/worklog")
    @Operation(summary = "Log work against a task")
    fun logWork(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @Valid @RequestBody
        request: LogWorkRequest,
    ): ResponseEntity<WorkLogEntryResponse> {
        val entry = workLogCommands.logWork(taskId, request.toCommand(), CurrentUser.actorOf(jwt))
        return ResponseEntity.status(HttpStatus.CREATED).body(entry)
    }

    @PutMapping("/worklog/{entryId}")
    @Operation(summary = "Edit a work log entry")
    fun editEntry(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable entryId: UUID,
        @Valid @RequestBody
        request: UpdateWorkLogRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<WorkLogEntryResponse> {
        val entry =
            workLogCommands.editEntry(
                entryId = entryId,
                command = request.toCommand(),
                actor = CurrentUser.actorOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ResponseEntity.ok(entry)
    }

    @DeleteMapping("/worklog/{entryId}")
    @Operation(summary = "Delete a work log entry")
    fun deleteEntry(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable entryId: UUID,
    ): ResponseEntity<Any> {
        workLogCommands.deleteEntry(entryId, CurrentUser.actorOf(jwt))
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
