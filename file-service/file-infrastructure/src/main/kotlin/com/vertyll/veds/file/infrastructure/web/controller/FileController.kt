package com.vertyll.veds.file.infrastructure.web.controller

import com.vertyll.veds.file.application.command.AttachFileCommand
import com.vertyll.veds.file.application.command.ConfirmUploadCommand
import com.vertyll.veds.file.application.dto.DownloadTicketResponse
import com.vertyll.veds.file.application.dto.FileResponse
import com.vertyll.veds.file.application.dto.UploadTicketResponse
import com.vertyll.veds.file.application.port.inbound.command.FileCommandUseCase
import com.vertyll.veds.file.application.port.inbound.query.FileQueryUseCase
import com.vertyll.veds.file.infrastructure.web.dto.AttachFileRequest
import com.vertyll.veds.file.infrastructure.web.dto.RequestUploadRequest
import com.vertyll.veds.file.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.security.AuthorizedInUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "Upload tickets and file metadata")
@AuthorizedInUseCase("FileCommandService.requireOwnedFile — a file answers only to the actor that uploaded it")
internal class FileController(
    private val commands: FileCommandUseCase,
    private val queries: FileQueryUseCase,
) {
    @PostMapping("/upload-ticket")
    @Operation(summary = "Ask for permission to upload and get a signed URL")
    fun requestUpload(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: RequestUploadRequest,
    ): ResponseEntity<UploadTicketResponse> {
        val ticket = commands.requestUpload(request.toCommand(), CurrentUser.actorOf(jwt))
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket)
    }

    @PostMapping("/{fileId}/confirm")
    @Operation(summary = "Confirm that the upload completed")
    fun confirmUpload(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable fileId: UUID,
    ): ResponseEntity<FileResponse> {
        val file = commands.confirmUpload(ConfirmUploadCommand(fileId), CurrentUser.actorOf(jwt))
        return ResponseEntity.ok(file)
    }

    @PostMapping("/{fileId}/attach")
    @Operation(summary = "Bind a confirmed file to the aggregate that owns it")
    fun attach(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable fileId: UUID,
        @Valid @RequestBody
        request: AttachFileRequest,
    ): ResponseEntity<FileResponse> {
        val file = commands.attach(AttachFileCommand(fileId, request.scopeId), CurrentUser.actorOf(jwt))
        return ResponseEntity.ok(file)
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get a file's metadata")
    fun getFile(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable fileId: UUID,
    ): ResponseEntity<FileResponse> = ResponseEntity.ok(queries.getFile(fileId, CurrentUser.actorOf(jwt)))

    @GetMapping("/{fileId}/download-ticket")
    @Operation(summary = "Get a short-lived URL to download the file")
    fun requestDownload(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable fileId: UUID,
    ): ResponseEntity<DownloadTicketResponse> {
        val ticket = queries.requestDownload(fileId, CurrentUser.actorOf(jwt))
        return ResponseEntity.ok(ticket)
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a file")
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable fileId: UUID,
    ): ResponseEntity<Any> {
        commands.delete(fileId, CurrentUser.actorOf(jwt))
        return ResponseEntity.ok().build()
    }
}
