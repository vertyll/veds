package com.vertyll.veds.task.application.dto

import java.time.Instant
import java.util.UUID

data class TaskCommentResponse(
    val id: UUID,
    val taskId: UUID,
    val author: TaskUserView,
    val content: String,
    val attachmentIds: Set<UUID>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long?,
)
