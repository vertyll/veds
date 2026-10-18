package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectTypeQueryUseCase {
    fun getAllTypes(language: LanguageTag): List<ProjectTypeResponse>

    fun getTypeById(
        id: UUID,
        language: LanguageTag,
    ): ProjectTypeResponse
}
