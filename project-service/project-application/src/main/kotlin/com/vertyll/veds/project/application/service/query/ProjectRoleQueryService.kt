package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.ProjectRoleResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectRoleQueryUseCase
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectRoleQueryService(
    private val roleRepository: ProjectRoleRepository,
) : ProjectRoleQueryUseCase {
    override fun getAllRoles(language: LanguageTag): List<ProjectRoleResponse> =
        roleRepository
            .findAll()
            .filter { it.isActive }
            .map { ProjectRoleResponse.from(it, language) }

    override fun getRoleById(
        id: UUID,
        language: LanguageTag,
    ): ProjectRoleResponse {
        val role = roleRepository.findById(id) ?: throw ApiException(ProjectError.ROLE_NOT_FOUND)
        return ProjectRoleResponse.from(role, language)
    }
}
