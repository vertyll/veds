package com.vertyll.veds.project.application.port.outbound

import com.vertyll.veds.project.application.dto.ProjectCategoryResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectMemberResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectStatusResponse
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.PageResult
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import java.util.UUID

interface ProjectQueryPort {
    fun findProject(projectId: UUID): ProjectResponse?

    fun searchProjects(
        criteria: ProjectSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<ProjectListItemResponse>

    fun findMembers(
        projectId: UUID,
        language: LanguageTag,
    ): List<ProjectMemberResponse>

    fun findCategories(
        projectId: UUID,
        language: LanguageTag,
    ): List<ProjectCategoryResponse>

    fun findStatuses(
        projectId: UUID,
        language: LanguageTag,
    ): List<ProjectStatusResponse>
}
