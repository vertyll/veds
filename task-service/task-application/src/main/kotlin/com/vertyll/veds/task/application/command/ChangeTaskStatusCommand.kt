package com.vertyll.veds.task.application.command

import java.util.UUID

data class ChangeTaskStatusCommand(
    val statusId: UUID?,
)
