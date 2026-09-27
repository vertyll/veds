package com.vertyll.veds.task.domain.model

import java.time.Instant
import java.util.UUID

data class ProjectCategoryRef(
    val categoryId: UUID,
    val projectId: UUID,
    val names: Map<String, String>,
    val color: String,
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(names.isNotEmpty()) { "a label projection must carry at least one name" }
    }

    fun resolve(language: LanguageTag): ResolvedLabel = names.resolveLabel(language)
}
