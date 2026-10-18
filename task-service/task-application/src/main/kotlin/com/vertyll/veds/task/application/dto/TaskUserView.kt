package com.vertyll.veds.task.application.dto

import java.util.UUID

data class TaskUserView(
    val id: UUID,
    val displayName: String,
    val avatarFileId: UUID?,
)
