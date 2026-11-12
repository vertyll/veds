package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.model.resolveFor
import java.util.UUID

data class ProjectStatusResponse(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val nameLanguage: String,
    val color: String,
    val isActive: Boolean,
    val translations: Set<TranslationResponse>,
    val version: Long?,
) {
    companion object {
        fun from(
            status: ProjectStatus,
            language: LanguageTag,
        ): ProjectStatusResponse =
            ProjectStatusResponse(
                id = status.id,
                projectId = status.projectId,
                name = status.translations.resolveFor(language).name,
                nameLanguage =
                    status.translations
                        .resolveFor(language)
                        .language.value,
                color = status.color,
                isActive = status.isActive,
                translations = status.translations.map(TranslationResponse::from).toSet(),
                version = status.version,
            )
    }
}
