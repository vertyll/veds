package com.vertyll.veds.task.application.command

import java.util.UUID

data class BatchDeleteTasksCommand(
    val taskIds: Set<UUID>,
)
