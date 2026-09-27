package com.vertyll.veds.task.application.dto

import java.util.UUID

data class TaskCategoryView(
    val id: UUID,
    val name: String,
    val nameLanguage: String,
    val color: String,
)
