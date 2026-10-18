package com.vertyll.veds.file.infrastructure.web.dto

import com.vertyll.veds.file.application.command.RequestUploadCommand
import com.vertyll.veds.file.domain.model.FileScope
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.util.UUID

data class RequestUploadRequest(
    @field:NotBlank(message = "validation.file.name_required")
    val originalName: String = "",
    @field:NotBlank(message = "validation.file.content_type_required")
    val contentType: String = "",
    @field:Positive(message = "validation.file.size_positive")
    val declaredSizeBytes: Long = 0,
    val scope: FileScope = FileScope.TASK_ATTACHMENT,
    val scopeId: UUID? = null,
) {
    fun toCommand(): RequestUploadCommand =
        RequestUploadCommand(
            originalName = originalName,
            contentType = contentType,
            declaredSizeBytes = declaredSizeBytes,
            scope = scope,
            scopeId = scopeId,
        )
}
