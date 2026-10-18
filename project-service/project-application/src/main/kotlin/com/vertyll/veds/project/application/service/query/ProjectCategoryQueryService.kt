package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectCategoryQueryUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectPermission
import java.util.UUID

class ProjectCategoryQueryService(
    private val queryPort: ProjectQueryPort,
    private val authorization: ProjectAuthorizationService,
) : ProjectCategoryQueryUseCase {
    override fun getCategories(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): List<ProjectCategoryResponse> {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return queryPort.findCategories(projectId, language)
    }
}
