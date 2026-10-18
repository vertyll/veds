package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectMembershipQueryUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.ProjectPermission
import java.util.UUID

class ProjectMembershipQueryService(
    private val queryPort: ProjectQueryPort,
    private val authorization: ProjectAuthorizationService,
) : ProjectMembershipQueryUseCase {
    override fun getMembers(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): List<ProjectMemberResponse> {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        return queryPort.findMembers(projectId, language)
    }

    override fun getEffectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<String> = authorization.effectivePermissions(projectId, actorId)
}
