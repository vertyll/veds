package com.vertyll.veds.task.application.port.inbound.command

import com.vertyll.veds.task.application.command.CreateCommentCommand
import com.vertyll.veds.task.application.command.UpdateCommentCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.dto.TaskCommentResponse
import java.util.UUID

interface TaskCommentCommandUseCase {
    fun addComment(
        taskId: UUID,
        command: CreateCommentCommand,
        actor: Actor,
    ): TaskCommentResponse

    fun editComment(
        commentId: UUID,
        command: UpdateCommentCommand,
        actor: Actor,
        version: Long? = null,
    ): TaskCommentResponse

    fun deleteComment(
        commentId: UUID,
        actor: Actor,
    )
}
