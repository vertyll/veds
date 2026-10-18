package com.vertyll.veds.task.application.command

import java.util.UUID

data class CreateCommentCommand(
    val content: String,
    val attachmentIds: Set<UUID>,
)
