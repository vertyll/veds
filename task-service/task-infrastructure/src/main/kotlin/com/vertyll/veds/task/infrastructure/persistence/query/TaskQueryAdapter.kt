package com.vertyll.veds.task.infrastructure.persistence.query

import com.vertyll.veds.task.application.dto.TaskCategoryView
import com.vertyll.veds.task.application.dto.TaskCommentResponse
import com.vertyll.veds.task.application.dto.TaskListItemResponse
import com.vertyll.veds.task.application.dto.TaskUserView
import com.vertyll.veds.task.application.dto.WorkLogEntryResponse
import com.vertyll.veds.task.application.dto.WorkLogPageResponse
import com.vertyll.veds.task.application.port.outbound.TaskQueryPort
import com.vertyll.veds.task.domain.model.LanguageTag
import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.TaskPriority
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.model.TaskSortField
import com.vertyll.veds.task.domain.model.WorkLogVisibility
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
@Suppress("LongMethod")
internal class TaskQueryAdapter : TaskQueryPort {
    private companion object {
        private const val COMMENT_ID = 0
        private const val COMMENT_TASK_ID = 1
        private const val COMMENT_AUTHOR_ID = 2
        private const val COMMENT_CONTENT = 3
        private const val COMMENT_CREATED_AT = 4
        private const val COMMENT_UPDATED_AT = 5
        private const val COMMENT_VERSION = 6
        private const val COMMENT_AUTHOR_EMAIL = 7
        private const val COMMENT_AUTHOR_FIRST_NAME = 8
        private const val COMMENT_AUTHOR_LAST_NAME = 9
        private const val COMMENT_AUTHOR_AVATAR = 10

        private const val WORK_LOG_ID = 0
        private const val WORK_LOG_TASK_ID = 1
        private const val WORK_LOG_AUTHOR_ID = 2
        private const val WORK_LOG_MINUTES = 3
        private const val WORK_LOG_WORKED_ON = 4
        private const val WORK_LOG_DESCRIPTION = 5
        private const val WORK_LOG_CREATED_AT = 6
        private const val WORK_LOG_UPDATED_AT = 7
        private const val WORK_LOG_VERSION = 8
        private const val WORK_LOG_AUTHOR_EMAIL = 9
        private const val WORK_LOG_AUTHOR_FIRST_NAME = 10
        private const val WORK_LOG_AUTHOR_LAST_NAME = 11
        private const val WORK_LOG_AUTHOR_AVATAR = 12
        private const val WORK_LOG_HIDDEN = 13

        private const val ASSIGNEE_TASK_ID = 0
        private const val ASSIGNEE_USER_ID = 1
        private const val ASSIGNEE_EMAIL = 2
        private const val ASSIGNEE_FIRST_NAME = 3
        private const val ASSIGNEE_LAST_NAME = 4
        private const val ASSIGNEE_AVATAR = 5
    }

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun searchTasks(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
        language: LanguageTag,
    ): PageResult<TaskListItemResponse> {
        val where =
            """
            WHERE t.projectId = :projectId
            AND (:onlyActive = FALSE OR t.isActive = TRUE)
            AND (:statusId IS NULL OR t.statusId = :statusId)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:categoryId IS NULL OR :categoryId MEMBER OF t.categoryIds)
            AND (:assigneeId IS NULL OR :assigneeId MEMBER OF t.assigneeIds)
            AND (
                :searchTerm IS NULL
                OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                OR LOWER(COALESCE(t.description, '')) LIKE
                   LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
            )
            """

        val order =
            when (criteria.sortBy) {
                TaskSortField.CREATED_AT -> "t.createdAt"
                TaskSortField.UPDATED_AT -> "t.updatedAt"
                TaskSortField.PRIORITY -> "t.priority"
                TaskSortField.NAME -> "t.name"
            } + if (criteria.sortDescending) " DESC" else " ASC"

        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT t.id, t.projectId, t.number, t.name, t.priority, t.statusId,
                           t.workedMinutes, t.createdAt, t.updatedAt, t.version,
                           (SELECT COUNT(c) FROM TaskCommentJpaEntity c WHERE c.taskId = t.id)
                    FROM TaskJpaEntity t
                    $where
                    ORDER BY $order
                    """,
                    Array<Any>::class.java,
                ).applyCriteria(criteria)
                .setFirstResult(pageRequest.offset.toInt())
                .setMaxResults(pageRequest.size)
                .resultList

        val total =
            entityManager
                .createQuery("SELECT COUNT(t) FROM TaskJpaEntity t $where", Long::class.javaObjectType)
                .applyCriteria(criteria)
                .singleResult

        val taskIds = rows.map { it[0] as UUID }
        val categoriesByTask = categoriesFor(taskIds, criteria.projectId, language)
        val assigneesByTask = assigneesFor(taskIds)
        val statuses = statusesFor(criteria.projectId, language)

        return PageResult(
            content =
                rows.map { r ->
                    val id = r[0] as UUID
                    val statusId = r[5] as UUID?
                    val status = statusId?.let { statuses[it] }
                    TaskListItemResponse(
                        id = id,
                        projectId = r[1] as UUID,
                        number = r[2] as Int,
                        name = r[3] as String,
                        priority = r[4] as TaskPriority,
                        statusId = statusId,
                        statusName = status?.name,
                        statusColor = status?.color,
                        categories = categoriesByTask[id].orEmpty(),
                        assignees = assigneesByTask[id].orEmpty(),
                        commentCount = (r[10] as Long).toInt(),
                        workedMinutes = r[6] as Int,
                        createdAt = r[7] as Instant,
                        updatedAt = r[8] as Instant,
                        version = r[9] as Long?,
                    )
                },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = total,
        )
    }

    override fun findComments(taskId: UUID): List<TaskCommentResponse> {
        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT c.id, c.taskId, c.authorId, c.content, c.createdAt, c.updatedAt, c.version,
                           u.email, u.firstName, u.lastName, u.avatarFileId
                    FROM TaskCommentJpaEntity c
                    LEFT JOIN UserRefJpaEntity u ON u.userId = c.authorId
                    WHERE c.taskId = :taskId
                    ORDER BY c.createdAt
                    """,
                    Array<Any>::class.java,
                ).setParameter("taskId", taskId)
                .resultList

        val attachments = attachmentsFor(rows.map { it[0] as UUID })

        return rows.map { r ->
            val id = r[COMMENT_ID] as UUID
            val email = r[COMMENT_AUTHOR_EMAIL] as String?
            TaskCommentResponse(
                id = id,
                taskId = r[COMMENT_TASK_ID] as UUID,
                author =
                    TaskUserView(
                        id = r[COMMENT_AUTHOR_ID] as UUID,
                        displayName =
                            listOfNotNull(r[COMMENT_AUTHOR_FIRST_NAME] as String?, r[COMMENT_AUTHOR_LAST_NAME] as String?)
                                .joinToString(" ")
                                .ifBlank { email ?: (r[COMMENT_AUTHOR_ID] as UUID).toString() },
                        avatarFileId = r[COMMENT_AUTHOR_AVATAR] as UUID?,
                    ),
                content = r[COMMENT_CONTENT] as String,
                attachmentIds = attachments[id].orEmpty(),
                createdAt = r[COMMENT_CREATED_AT] as Instant,
                updatedAt = r[COMMENT_UPDATED_AT] as Instant,
                version = r[COMMENT_VERSION] as Long?,
            )
        }
    }

    override fun findWorkLog(
        taskId: UUID,
        readerId: UUID,
        readsHidden: Boolean,
        visibility: WorkLogVisibility,
        pageRequest: PageRequest,
    ): WorkLogPageResponse {
        val readableByCaller =
            if (readsHidden) "" else "AND (e.hidden = FALSE OR e.authorId = :readerId)"
        val chosen =
            when (visibility) {
                WorkLogVisibility.ALL -> ""
                WorkLogVisibility.HIDDEN -> "AND e.hidden = TRUE"
                WorkLogVisibility.VISIBLE -> "AND e.hidden = FALSE"
            }

        val total =
            entityManager
                .createQuery(
                    """
                    SELECT COUNT(e) FROM WorkLogEntryJpaEntity e
                    WHERE e.taskId = :taskId $readableByCaller $chosen
                    """,
                    Long::class.javaObjectType,
                ).setParameter("taskId", taskId)
                .also { if (!readsHidden) it.setParameter("readerId", readerId) }
                .singleResult

        val totalMinutes =
            entityManager
                .createQuery(
                    """
                    SELECT COALESCE(SUM(e.minutes), 0) FROM WorkLogEntryJpaEntity e
                    WHERE e.taskId = :taskId $readableByCaller $chosen
                    """,
                    Long::class.javaObjectType,
                ).setParameter("taskId", taskId)
                .also { if (!readsHidden) it.setParameter("readerId", readerId) }
                .singleResult
                .toInt()

        val rows =
            entityManager
                .createQuery(
                    """
                    SELECT e.id, e.taskId, e.authorId, e.minutes, e.workedOn, e.description,
                           e.createdAt, e.updatedAt, e.version,
                           u.email, u.firstName, u.lastName, u.avatarFileId, e.hidden
                    FROM WorkLogEntryJpaEntity e
                    LEFT JOIN UserRefJpaEntity u ON u.userId = e.authorId
                    WHERE e.taskId = :taskId $readableByCaller $chosen
                    ORDER BY e.workedOn DESC, e.createdAt DESC
                    """,
                    Array<Any>::class.java,
                ).setParameter("taskId", taskId)
                .also { if (!readsHidden) it.setParameter("readerId", readerId) }
                .setFirstResult(pageRequest.page * pageRequest.size)
                .setMaxResults(pageRequest.size)
                .resultList

        val content =
            rows.map { r ->
                val email = r[WORK_LOG_AUTHOR_EMAIL] as String?
                WorkLogEntryResponse(
                    id = r[WORK_LOG_ID] as UUID,
                    taskId = r[WORK_LOG_TASK_ID] as UUID,
                    author =
                        TaskUserView(
                            id = r[WORK_LOG_AUTHOR_ID] as UUID,
                            displayName =
                                listOfNotNull(r[WORK_LOG_AUTHOR_FIRST_NAME] as String?, r[WORK_LOG_AUTHOR_LAST_NAME] as String?)
                                    .joinToString(" ")
                                    .ifBlank { email ?: (r[WORK_LOG_AUTHOR_ID] as UUID).toString() },
                            avatarFileId = r[WORK_LOG_AUTHOR_AVATAR] as UUID?,
                        ),
                    minutes = r[WORK_LOG_MINUTES] as Int,
                    workedOn = r[WORK_LOG_WORKED_ON] as LocalDate,
                    description = r[WORK_LOG_DESCRIPTION] as String?,
                    hidden = r[WORK_LOG_HIDDEN] as Boolean,
                    createdAt = r[WORK_LOG_CREATED_AT] as Instant,
                    updatedAt = r[WORK_LOG_UPDATED_AT] as Instant,
                    version = r[WORK_LOG_VERSION] as Long?,
                )
            }

        return WorkLogPageResponse(
            content = content,
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = total,
            totalMinutes = totalMinutes,
        )
    }

    private fun categoriesFor(
        taskIds: List<UUID>,
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, List<TaskCategoryView>> {
        if (taskIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val links =
            entityManager
                .createNativeQuery("SELECT task_id, category_id FROM task_category WHERE task_id IN (:ids)")
                .setParameter("ids", taskIds)
                .resultList as List<Array<Any?>>

        val labels = categoryLabels(projectId, language)

        return links
            .groupBy { it[0] as UUID }
            .mapValues { (_, rows) ->
                rows.mapNotNull { row ->
                    val categoryId = row[1] as UUID
                    labels[categoryId]?.let { label ->
                        TaskCategoryView(
                            id = categoryId,
                            name = label.name,
                            nameLanguage = label.language,
                            color = label.color,
                        )
                    }
                }
            }
    }

    private fun assigneesFor(taskIds: List<UUID>): Map<UUID, List<TaskUserView>> {
        if (taskIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    """
                    SELECT a.task_id, a.user_id, u.email, u.first_name, u.last_name, u.avatar_file_id
                    FROM task_assignee a
                    LEFT JOIN user_ref u ON u.user_id = a.user_id
                    WHERE a.task_id IN (:ids)
                    """,
                ).setParameter("ids", taskIds)
                .resultList as List<Array<Any?>>

        return rows
            .groupBy { it[ASSIGNEE_TASK_ID] as UUID }
            .mapValues { (_, group) ->
                group.map { r ->
                    val userId = r[ASSIGNEE_USER_ID] as UUID
                    val email = r[ASSIGNEE_EMAIL] as String?
                    TaskUserView(
                        id = userId,
                        displayName =
                            listOfNotNull(r[ASSIGNEE_FIRST_NAME] as String?, r[ASSIGNEE_LAST_NAME] as String?)
                                .joinToString(" ")
                                .ifBlank { email ?: userId.toString() },
                        avatarFileId = r[ASSIGNEE_AVATAR] as UUID?,
                    )
                }
            }
    }

    private fun attachmentsFor(commentIds: List<UUID>): Map<UUID, Set<UUID>> {
        if (commentIds.isEmpty()) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    "SELECT comment_id, attachment_id FROM task_comment_attachment WHERE comment_id IN (:ids)",
                ).setParameter("ids", commentIds)
                .resultList as List<Array<Any?>>

        return rows.groupBy { it[0] as UUID }.mapValues { (_, g) -> g.map { it[1] as UUID }.toSet() }
    }

    private fun categoryLabels(
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, ResolvedLabelRow> = labelsOf("project_category_ref", "category_id", projectId, language)

    private fun statusesFor(
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, ResolvedLabelRow> = labelsOf("project_status_ref", "status_id", projectId, language)

    private fun labelsOf(
        table: String,
        idColumn: String,
        projectId: UUID,
        language: LanguageTag,
    ): Map<UUID, ResolvedLabelRow> {
        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(
                    """
                    -- LEFT JOIN with a fallback row: a project-owned label may
                    -- not exist in the requested language, and a board that drops
                    -- the chip entirely would look like the category was removed.
                    SELECT r.$idColumn,
                           COALESCE(requested.name, any_name.name),
                           COALESCE(requested.language, any_name.language),
                           r.color
                    FROM $table r
                    LEFT JOIN ${table}_name requested
                        ON requested.$idColumn = r.$idColumn AND requested.language = :language
                    LEFT JOIN LATERAL (
                        SELECT n.name, n.language FROM ${table}_name n
                        WHERE n.$idColumn = r.$idColumn
                        ORDER BY n.language
                        LIMIT 1
                    ) any_name ON TRUE
                    WHERE r.project_id = :projectId
                    """,
                ).setParameter("projectId", projectId)
                .setParameter("language", language.value)
                .resultList as List<Array<Any?>>

        return rows.associate {
            (it[0] as UUID) to
                ResolvedLabelRow(
                    name = it[1] as String,
                    language = it[2] as String,
                    color = it[3] as String,
                )
        }
    }

    private fun <T> TypedQuery<T>.applyCriteria(criteria: TaskSearchCriteria) =
        setParameter("projectId", criteria.projectId)
            .setParameter("onlyActive", criteria.onlyActive)
            .setParameter("statusId", criteria.statusId)
            .setParameter("priority", criteria.priority)
            .setParameter("categoryId", criteria.categoryId)
            .setParameter("assigneeId", criteria.assigneeId)
            .setParameter("searchTerm", criteria.searchTerm)
}
