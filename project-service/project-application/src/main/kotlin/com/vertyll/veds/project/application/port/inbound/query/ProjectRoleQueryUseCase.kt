package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.ProjectRoleResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectRoleQueryUseCase {
    fun getAllRoles(language: LanguageTag): List<ProjectRoleResponse>

    fun getRoleById(
        id: UUID,
        language: LanguageTag,
    ): ProjectRoleResponse
}
