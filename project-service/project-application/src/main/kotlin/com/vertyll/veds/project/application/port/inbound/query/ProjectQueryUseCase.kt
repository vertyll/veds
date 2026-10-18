package com.vertyll.veds.project.application.port.inbound.query

import com.vertyll.veds.project.application.dto.PagedResponse
import com.vertyll.veds.project.application.dto.ProjectDetailsResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectSearchParams
import com.vertyll.veds.project.domain.model.LanguageTag
import java.util.UUID

interface ProjectQueryUseCase {
    fun getProject(
        projectId: UUID,
        actorId: UUID,
    ): ProjectResponse

    fun getProjectDetails(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): ProjectDetailsResponse

    fun searchProjects(
        params: ProjectSearchParams,
        actorId: UUID,
    ): PagedResponse<ProjectListItemResponse>
}
