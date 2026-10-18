package com.vertyll.veds.project.application.port.inbound.command

import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectMembershipCommandUseCase {
    fun updateMemberRole(
        projectId: UUID,
        memberId: UUID,
        command: UpdateMemberRoleCommand,
        actorId: UUID,
        language: LanguageTag,
        version: Long? = null,
    ): ProjectMemberResponse

    fun removeMember(
        projectId: UUID,
        memberId: UUID,
        actorId: UUID,
    )
}
