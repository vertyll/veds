package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectMembershipQueryUseCase {
    fun getMembers(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): List<ProjectMemberResponse>

    fun getEffectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<String>
}
