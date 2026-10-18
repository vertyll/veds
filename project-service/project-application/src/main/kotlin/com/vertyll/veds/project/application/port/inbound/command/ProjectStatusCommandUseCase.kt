package com.vertyll.veds.project.application.port.inbound.command

import com.vertyll.veds.project.application.command.CreateStatusCommand
import com.vertyll.veds.project.application.command.UpdateStatusCommand
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectStatusCommandUseCase {
    fun createStatus(
        projectId: UUID,
        command: CreateStatusCommand,
        actorId: UUID,
        language: LanguageTag,
    ): ProjectStatusResponse

    fun updateStatus(
        projectId: UUID,
        statusId: UUID,
        command: UpdateStatusCommand,
        actorId: UUID,
        language: LanguageTag,
        version: Long? = null,
    ): ProjectStatusResponse

    fun deleteStatus(
        projectId: UUID,
        statusId: UUID,
        actorId: UUID,
    )
}
