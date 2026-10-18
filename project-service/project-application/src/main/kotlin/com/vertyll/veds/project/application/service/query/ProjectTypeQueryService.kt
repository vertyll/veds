package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectTypeQueryUseCase
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectTypeQueryService(
    private val typeRepository: ProjectTypeRepository,
) : ProjectTypeQueryUseCase {
    override fun getAllTypes(language: LanguageTag): List<ProjectTypeResponse> =
        typeRepository
            .findAll()
            .filter { it.isActive }
            .map { ProjectTypeResponse.from(it, language) }

    override fun getTypeById(
        id: UUID,
        language: LanguageTag,
    ): ProjectTypeResponse {
        val type = typeRepository.findById(id) ?: throw ApiException(ProjectError.TYPE_NOT_FOUND)
        return ProjectTypeResponse.from(type, language)
    }
}
