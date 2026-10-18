package com.vertyll.veds.project.application.service.query

import com.vertyll.veds.project.application.dto.PagedResponse
import com.vertyll.veds.project.application.dto.PaginationMeta
import com.vertyll.veds.project.application.dto.ProjectDetailsResponse
import com.vertyll.veds.project.application.dto.ProjectListItemResponse
import com.vertyll.veds.project.application.dto.ProjectResponse
import com.vertyll.veds.project.application.dto.ProjectSearchParams
import com.vertyll.veds.project.application.dto.ProjectTypeResponse
import com.vertyll.veds.project.application.port.inbound.query.ProjectQueryUseCase
import com.vertyll.veds.project.application.port.outbound.ProjectQueryPort
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.LanguageTag
import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.sharederror.ApiException
import java.util.UUID

class ProjectQueryService(
    private val queryPort: ProjectQueryPort,
    private val typeRepository: ProjectTypeRepository,
    private val authorization: ProjectAuthorizationService,
) : ProjectQueryUseCase {
    override fun getProject(
        projectId: UUID,
        actorId: UUID,
    ): ProjectResponse {
        authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)
        val project = queryPort.findProject(projectId) ?: throw ApiException(ProjectError.PROJECT_NOT_FOUND)
        return project.copy(permissions = authorization.effectivePermissions(projectId, actorId))
    }

    override fun getProjectDetails(
        projectId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): ProjectDetailsResponse {
        val project = authorization.requirePermission(projectId, actorId, ProjectPermission.VIEW_PROJECT)

        return ProjectDetailsResponse(
            project = queryPort.findProject(projectId) ?: throw ApiException(ProjectError.PROJECT_NOT_FOUND),
            type = project.typeId?.let { typeRepository.findById(it) }?.let { ProjectTypeResponse.from(it, language) },
            members = queryPort.findMembers(projectId, language),
            categories = queryPort.findCategories(projectId, language),
            statuses = queryPort.findStatuses(projectId, language),
            permissions = authorization.effectivePermissions(projectId, actorId),
            currentUserId = actorId,
        )
    }

    override fun searchProjects(
        params: ProjectSearchParams,
        actorId: UUID,
    ): PagedResponse<ProjectListItemResponse> {
        val criteria =
            ProjectSearchCriteria(
                requesterId = actorId,
                searchTerm = params.searchTerm?.takeIf { it.isNotBlank() },
                typeId = params.typeId,
                onlyActive = params.onlyActive,
                includePublic = params.includePublic,
                sortBy = params.sortBy,
                sortDescending = params.sortDescending,
            )

        val page = queryPort.searchProjects(criteria, PageRequest(params.page, params.size))
        return PagedResponse(
            items = page.content.map { it.copy(permissions = authorization.effectivePermissions(it.id, actorId)) },
            pagination =
                PaginationMeta(
                    total = page.totalElements,
                    page = page.page,
                    pageSize = page.size,
                    totalPages = page.totalPages,
                    hasMore = page.page + 1 < page.totalPages,
                ),
        )
    }
}
