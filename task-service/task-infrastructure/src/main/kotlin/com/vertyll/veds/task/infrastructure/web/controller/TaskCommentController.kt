package com.vertyll.veds.task.infrastructure.web.controller

import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.shared.web.security.AuthorizedInUseCase
import com.vertyll.veds.task.application.dto.TaskCommentResponse
import com.vertyll.veds.task.application.port.inbound.command.TaskCommentCommandUseCase
import com.vertyll.veds.task.application.port.inbound.query.TaskCommentQueryUseCase
import com.vertyll.veds.task.infrastructure.web.dto.CreateCommentRequest
import com.vertyll.veds.task.infrastructure.web.dto.UpdateCommentRequest
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tasks")
@Tag(name = "Task comments", description = "Comment management on tasks")
@AuthorizedInUseCase("TaskAuthorizationService — a comment is reachable only through a task the caller may see")
internal class TaskCommentController(
    private val commentCommands: TaskCommentCommandUseCase,
    private val commentQueries: TaskCommentQueryUseCase,
) {
    @GetMapping("/{taskId}/comments")
    @Operation(summary = "List comments on a task")
    fun getComments(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
    ): ResponseEntity<List<TaskCommentResponse>> {
        val comments = commentQueries.getComments(taskId, CurrentUser.idOf(jwt))
        return ResponseEntity.ok(comments)
    }

    @PostMapping("/{taskId}/comments")
    @Operation(summary = "Add a comment to a task")
    fun addComment(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable taskId: UUID,
        @Valid @RequestBody
        request: CreateCommentRequest,
    ): ResponseEntity<TaskCommentResponse> {
        val comment = commentCommands.addComment(taskId, request.toCommand(), CurrentUser.actorOf(jwt))
        return ResponseEntity.status(HttpStatus.CREATED).body(comment)
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Edit a comment")
    fun editComment(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable commentId: UUID,
        @Valid @RequestBody
        request: UpdateCommentRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<TaskCommentResponse> {
        val comment =
            commentCommands.editComment(
                commentId = commentId,
                command = request.toCommand(),
                actor = CurrentUser.actorOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ResponseEntity.ok(comment)
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    fun deleteComment(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable commentId: UUID,
    ): ResponseEntity<Any> {
        commentCommands.deleteComment(commentId, CurrentUser.actorOf(jwt))
        return ResponseEntity.ok().build()
    }
}
