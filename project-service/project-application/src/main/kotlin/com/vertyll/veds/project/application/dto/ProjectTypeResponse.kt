package com.vertyll.veds.project.application.dto

import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import java.util.UUID

data class ProjectTypeResponse(
    val id: UUID,
    val code: ProjectTypeCode,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val version: Long?,
) {
    companion object {
        fun from(
            type: ProjectType,
            language: LanguageTag,
        ): ProjectTypeResponse =
            ProjectTypeResponse(
                id = type.id,
                code = type.code,
                name = type.translationFor(language).name,
                description = type.translationFor(language).description,
                isActive = type.isActive,
                version = type.version,
            )
    }
}
