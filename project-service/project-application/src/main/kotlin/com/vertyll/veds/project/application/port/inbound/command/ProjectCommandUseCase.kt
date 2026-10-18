package com.vertyll.veds.project.application.port.inbound.command

import com.vertyll.veds.project.application.command.CreateProjectCommand
import com.vertyll.veds.project.application.command.UpdateProjectCommand
import com.vertyll.veds.project.application.dto.Actor
import com.vertyll.veds.project.application.dto.ProjectResponse
import java.util.UUID

interface ProjectCommandUseCase {
    fun createProject(
        command: CreateProjectCommand,
        actor: Actor,
    ): ProjectResponse

    fun updateProject(
        projectId: UUID,
        command: UpdateProjectCommand,
        actorId: UUID,
        version: Long? = null,
    ): ProjectResponse

    fun archiveProject(
        projectId: UUID,
        actorId: UUID,
        version: Long? = null,
    )
}
