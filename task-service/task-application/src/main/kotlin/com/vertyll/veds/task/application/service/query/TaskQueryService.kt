package com.vertyll.veds.task.application.service.query

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.dto.PagedResponse
import com.vertyll.veds.task.application.dto.PaginationMeta
import com.vertyll.veds.task.application.dto.TaskCategoryView
import com.vertyll.veds.task.application.dto.TaskDetailsResponse
import com.vertyll.veds.task.application.dto.TaskListItemResponse
import com.vertyll.veds.task.application.dto.TaskResponse
import com.vertyll.veds.task.application.dto.TaskSearchParams
import com.vertyll.veds.task.application.dto.TaskUserView
import com.vertyll.veds.task.application.port.inbound.query.TaskQueryUseCase
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.ProjectRef
import com.vertyll.veds.task.domain.model.TaskPermission
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.repository.ProjectDirectoryRepository
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import java.util.UUID

class TaskQueryService(
    private val queryPort: TaskQueryPort,
    private val projectDirectory: ProjectDirectoryRepository,
    private val userDirectory: UserDirectoryRepository,
    private val authorization: TaskAuthorizationService,
) : TaskQueryUseCase {
    override fun searchTasks(
        projectId: UUID,
        params: TaskSearchParams,
        actorId: UUID,
        language: LanguageTag,
    ): PagedResponse<TaskListItemResponse> {
        authorization.requireProjectPermission(projectId, actorId, TaskPermission.VIEW_TASKS)

        val criteria =
            TaskSearchCriteria(
                projectId = projectId,
                searchTerm = params.searchTerm?.takeIf { it.isNotBlank() },
                statusId = params.statusId,
                categoryId = params.categoryId,
                assigneeId = params.assigneeId,
                priority = params.priority,
                onlyActive = params.onlyActive,
                sortBy = params.sortBy,
                sortDescending = params.sortDescending,
            )

        val page = queryPort.searchTasks(criteria, PageRequest(params.page, params.size), language)

        return PagedResponse(
            items = page.content,
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

    override fun getTaskDetails(
        taskId: UUID,
        actorId: UUID,
        language: LanguageTag,
    ): TaskDetailsResponse {
        val task = authorization.requireTaskPermission(taskId, actorId, TaskPermission.VIEW_TASKS)

        val categories =
            projectDirectory
                .findCategories(task.projectId)
                .filter { it.categoryId in task.categoryIds }
                .map { ref ->
                    val resolved = ref.resolve(language)
                    TaskCategoryView(
                        id = ref.categoryId,
                        name = resolved.name,
                        nameLanguage = resolved.language,
                        color = ref.color,
                    )
                }

        val statusName =
            task.statusId?.let { statusId ->
                projectDirectory
                    .findStatuses(task.projectId)
                    .firstOrNull { it.statusId == statusId }
                    ?.resolve(language)
                    ?.name
            }

        val people = userDirectory.findAllByIds(task.assigneeIds + task.createdBy).associateBy { it.userId }
        val toView = { userId: UUID ->
            people[userId]?.let { TaskUserView(id = it.userId, displayName = it.displayName, avatarFileId = it.avatarFileId) }
        }
        val assignees = task.assigneeIds.mapNotNull(toView)

        return TaskDetailsResponse(
            task = TaskResponse.from(task),
            statusName = statusName,
            categories = categories,
            assignees = assignees,
            createdBy = toView(task.createdBy),
            comments = queryPort.findComments(taskId),
            permissions = authorization.effectivePermissions(task.projectId, actorId),
            hiddenWorkLogEnabled = projectOf(task.projectId).hiddenWorkLogEnabled,
        )
    }

    override fun getEffectivePermissions(
        projectId: UUID,
        actorId: UUID,
    ): Set<TaskPermission> = authorization.effectivePermissions(projectId, actorId)

    private fun projectOf(projectId: UUID): ProjectRef =
        projectDirectory.findProject(projectId)
            ?: throw ApiException(TaskError.PROJECT_NOT_KNOWN, mapOf("projectId" to projectId.toString()))
}
