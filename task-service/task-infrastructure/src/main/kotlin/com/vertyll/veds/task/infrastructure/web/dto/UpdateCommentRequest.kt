package com.vertyll.veds.task.infrastructure.web.dto

import com.vertyll.veds.task.application.command.UpdateCommentCommand
import jakarta.validation.constraints.NotBlank

data class UpdateCommentRequest(
    @field:NotBlank(message = "validation.task.comment_content_required")
    val content: String = "",
) {
    fun toCommand(): UpdateCommentCommand = UpdateCommentCommand(content = content)
}
