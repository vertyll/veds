package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.model.Translation
import com.vertyll.veds.project.domain.model.resolveFor
import java.util.UUID

data class ProjectCategoryResponse(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val nameLanguage: String,
    val color: String,
    val isActive: Boolean,
    val translations: Set<Translation>,
    val version: Long?,
) {
    companion object {
        fun from(
            category: ProjectCategory,
            language: LanguageTag,
        ): ProjectCategoryResponse =
            ProjectCategoryResponse(
                id = category.id,
                projectId = category.projectId,
                name = category.translations.resolveFor(language).name,
                nameLanguage =
                    category.translations
                        .resolveFor(language)
                        .language.value,
                color = category.color,
                isActive = category.isActive,
                translations = category.translations,
                version = category.version,
            )
    }
}
