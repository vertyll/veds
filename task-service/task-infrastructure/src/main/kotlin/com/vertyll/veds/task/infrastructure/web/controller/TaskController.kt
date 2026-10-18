package com.vertyll.veds.task.infrastructure.web.controller

import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.shared.web.security.AuthorizedInUseCase
import com.vertyll.veds.task.application.dto.PagedResponse
import com.vertyll.veds.task.application.dto.TaskDetailsResponse
import com.vertyll.veds.task.application.dto.TaskListItemResponse
import com.vertyll.veds.task.application.dto.TaskResponse
import com.vertyll.veds.task.application.dto.TaskSearchParams
import com.vertyll.veds.task.application.port.inbound.command.TaskCommandUseCase
import com.vertyll.veds.task.application.port.inbound.query.TaskQueryUseCase
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.TaskPriority
import com.vertyll.veds.task.domain.model.TaskSortField
import com.vertyll.veds.task.infrastructure.web.LanguageHeader
import com.vertyll.veds.task.infrastructure.web.dto.BatchDeleteTasksRequest
import com.vertyll.veds.task.infrastructure.web.dto.ChangeTaskStatusRequest
import com.vertyll.veds.task.infrastructure.web.dto.CreateTaskRequest
import com.vertyll.veds.task.infrastructure.web.dto.UpdateTaskRequest
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
import org.springframework.web.bind.annotation.PatchMapping
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
@Tag(name = "Tasks", description = "Task management APIs")
@Suppress("TooManyFunctions")
@AuthorizedInUseCase("TaskAuthorizationService — the decision needs project membership and the task's own state")
internal class TaskController(
    private val taskCommands: TaskCommandUseCase,
    private val taskQueries: TaskQueryUseCase,
) {
    private companion object {
        private const val DEFAULT_PAGE_SIZE = "25"
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List tasks in a project")
    @Suppress("LongParameterList")
    fun getTasks(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @RequestParam(required = false) searchTerm: String?,
        @RequestParam(required = false) statusId: UUID?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) assigneeId: UUID?,
        @RequestParam(required = false) priority: TaskPriority?,
        @RequestParam(defaultValue = "true") onlyActive: Boolean,
        @RequestParam(defaultValue = "CREATED_AT") sortBy: TaskSortField,
        @RequestParam(defaultValue = "true") sortDescending: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) size: Int,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<PagedResponse<TaskListItemResponse>> {
        val params =
            TaskSearchParams(
                searchTerm = searchTerm,
                statusId = statusId,
                categoryId = categoryId,
                assigneeId = assigneeId,
                priority = priority,
                onlyActive = onlyActive,
                sortBy = sortBy,
                sortDescending = sortDescending,
                page = page,
                size = size,
            )
        val tasks =
            taskQueries.searchTasks(
                projectId = projectId,
                params = params,
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return ResponseEntity.ok(tasks)
    }

    @PostMapping("/project/{projectId}")
    @Operation(summary = "Create a task in a project")
    fun createTask(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @Valid @RequestBody
        request: CreateTaskRequest,
    ): ResponseEntity<TaskResponse> {
        val task = taskCommands.createTask(request.toCommand(projectId), CurrentUser.actorOf(jwt))
        return ResponseEntity.status(HttpStatus.CREATED).body(task)
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get a task with comments, labels and the caller's permissions")
    fun getTask(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @RequestHeader(LanguageHeader.NAME, required = false) acceptLanguage: String?,
    ): ResponseEntity<TaskDetailsResponse> {
        val details =
            taskQueries.getTaskDetails(
                taskId = taskId,
                actorId = CurrentUser.idOf(jwt),
                language = CurrentUser.languageOf(acceptLanguage),
            )
        return withETag(details, details.task.version)
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update a task")
    fun updateTask(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @Valid @RequestBody
        request: UpdateTaskRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<TaskResponse> {
        val task =
            taskCommands.updateTask(
                taskId = taskId,
                command = request.toCommand(),
                actor = CurrentUser.actorOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return withETag(task, task.version)
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Move a task to another status")
    fun changeStatus(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @Valid @RequestBody
        request: ChangeTaskStatusRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<TaskResponse> {
        val task =
            taskCommands.changeStatus(
                taskId = taskId,
                command = request.toCommand(),
                actor = CurrentUser.actorOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return withETag(task, task.version)
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Archive a task")
    fun archiveTask(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<Any> {
        taskCommands.archiveTask(
            taskId = taskId,
            actor = CurrentUser.actorOf(jwt),
            version = ETagUtils.parseIfMatchToVersion(ifMatch),
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "Archive several tasks at once")
    fun archiveTasks(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: BatchDeleteTasksRequest,
    ): ResponseEntity<Int> {
        val archived = taskCommands.archiveTasks(request.toCommand(), CurrentUser.actorOf(jwt))
        return ResponseEntity.ok(archived)
    }

    @GetMapping("/project/{projectId}/permissions")
    @Operation(summary = "Get the caller's effective task permissions in a project")
    fun getPermissions(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
    ): ResponseEntity<Set<TaskPermission>> {
        val permissions = taskQueries.getEffectivePermissions(projectId, CurrentUser.idOf(jwt))
        return ResponseEntity.ok(permissions)
    }

    private fun <T : Any> withETag(
        body: T,
        version: Long?,
    ): ResponseEntity<T> {
        val etag = ETagUtils.buildWeakETag(version) ?: return ResponseEntity.ok(body)
        return ResponseEntity.ok().eTag(etag).body(body)
    }
}
