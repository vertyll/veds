package com.vertyll.veds.project.application.command

import java.util.UUID

data class UpdateProjectCommand(
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val typeId: UUID?,
    val iconFileId: UUID?,
    val hiddenWorkLogEnabled: Boolean = false,
)
