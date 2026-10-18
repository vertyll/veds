package com.vertyll.veds.project.application.command

import com.vertyll.veds.project.domain.model.Translation

data class UpdateStatusCommand(
    val color: String,
    val translations: Set<Translation>,
    val isActive: Boolean,
)
