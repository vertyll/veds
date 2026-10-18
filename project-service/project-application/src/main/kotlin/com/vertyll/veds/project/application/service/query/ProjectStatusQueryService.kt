package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectStatusQueryUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectPermission
import java.util.UUID

class ProjectStatusQueryService(
    private val queryPort: ProjectQueryPort,
    private val authorization: ProjectAuthorizationService,
) : ProjectStatusQueryUseCase {
    override fun getStatuses(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): List<ProjectStatusResponse> {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return queryPort.findStatuses(projectId, language)
    }
}
