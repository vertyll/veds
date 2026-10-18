package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.CreateCommentCommand
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateCommentRequest(
    @field:NotBlank(message = "validation.task.comment_content_required")
    val content: String = "",
    val attachmentIds: Set<UUID> = emptySet(),
) {
    fun toCommand(): CreateCommentCommand = CreateCommentCommand(content = content, attachmentIds = attachmentIds)
}
