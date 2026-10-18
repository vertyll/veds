package com.vertyll.veds.project.application.command

import com.vertyll.veds.project.domain.model.Translation

data class CreateStatusCommand(
    val color: String,
    val translations: Set<Translation>,
)
