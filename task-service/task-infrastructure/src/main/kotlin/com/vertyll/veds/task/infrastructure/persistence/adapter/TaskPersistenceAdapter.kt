package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.model.TaskSortField
import com.vertyll.veds.task.domain.repository.TaskRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.TaskJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.TaskJpaRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class TaskPersistenceAdapter(
    private val repository: TaskJpaRepository,
) : TaskRepository {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun save(task: Task): Task = repository.save(task.toJpaEntity()).toDomain()

    override fun saveAll(tasks: Collection<Task>): List<Task> = repository.saveAll(tasks.map { it.toJpaEntity() }).map { it.toDomain() }

    override fun findById(id: UUID): Task? = repository.findByIdOrNull(id)?.toDomain()

    override fun findAllByIds(ids: Collection<UUID>): List<Task> =
        if (ids.isEmpty()) emptyList() else repository.findAllById(ids).map { it.toDomain() }

    /**
     * The two interpolated fragments are fixed literals: `order` comes from an exhaustive
     * `when` over [TaskSortField], and `buildWhere` appends only constant text. Every value
     * the caller supplies is bound as a named parameter in `applyCriteria`, so nothing the
     * caller writes reaches the query text.
     */
    @Suppress("SqlSourceToSinkFlow")
    override fun search(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<Task> {
        val where = buildWhere(criteria)
        val order =
            when (criteria.sortBy) {
                TaskSortField.CREATED_AT -> "t.createdAt"
                TaskSortField.UPDATED_AT -> "t.updatedAt"
                TaskSortField.PRIORITY -> "t.priority"
                TaskSortField.NAME -> "t.name"
            } + if (criteria.sortDescending) " DESC" else " ASC"

        val rows =
            entityManager
                .createQuery("SELECT t FROM TaskJpaEntity t $where ORDER BY $order", TaskJpaEntity::class.java)
                .applyCriteria(criteria)
                .setFirstResult(pageRequest.offset.toInt())
                .setMaxResults(pageRequest.size)
                .resultList

        val total =
            entityManager
                .createQuery("SELECT COUNT(t) FROM TaskJpaEntity t $where", Long::class.javaObjectType)
                .applyCriteria(criteria)
                .singleResult

        return PageResult(
            content = rows.map { it.toDomain() },
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = total,
        )
    }

    override fun findAllByProjectId(projectId: UUID): List<Task> = repository.findAllByProjectId(projectId).map { it.toDomain() }

    override fun findAllByCategoryId(categoryId: UUID): List<Task> = repository.findAllByCategoryId(categoryId).map { it.toDomain() }

    override fun findAllByAttachmentId(attachmentId: UUID): List<Task> =
        repository.findAllByAttachmentIdsContaining(attachmentId).map { it.toDomain() }

    override fun findAllByStatusId(statusId: UUID): List<Task> = repository.findAllByStatusId(statusId).map { it.toDomain() }

    override fun highestNumberIn(projectId: UUID): Int = repository.highestNumberIn(projectId)

    override fun delete(id: UUID) = repository.deleteById(id)

    private fun <T> jakarta.persistence.TypedQuery<T>.applyCriteria(criteria: TaskSearchCriteria) =
        apply {
            setParameter("projectId", criteria.projectId)
            setParameter("onlyActive", criteria.onlyActive)
            setParameter("statusId", criteria.statusId)
            setParameter("priority", criteria.priority)
            setParameter("searchTerm", criteria.searchTerm)
            if (criteria.categoryId != null) setParameter("categoryId", criteria.categoryId)
            if (criteria.assigneeId != null) setParameter("assigneeId", criteria.assigneeId)
        }

    private companion object {
        fun buildWhere(criteria: TaskSearchCriteria): String =
            buildString {
                append(
                    """
                    WHERE t.projectId = :projectId
                    AND (:onlyActive = FALSE OR t.isActive = TRUE)
                    AND (:statusId IS NULL OR t.statusId = :statusId)
                    AND (:priority IS NULL OR t.priority = :priority)
                    AND (
                        :searchTerm IS NULL
                        OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                        OR LOWER(COALESCE(t.description, ''))
                            LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
                    )
                    """,
                )
                if (criteria.categoryId != null) append(" AND :categoryId MEMBER OF t.categoryIds")
                if (criteria.assigneeId != null) append(" AND :assigneeId MEMBER OF t.assigneeIds")
            }
    }
}

private fun Task.toJpaEntity() =
    TaskJpaEntity(
        id = this.id,
        projectId = this.projectId,
        number = this.number,
        name = this.name,
        description = this.description,
        priceEstimation = this.priceEstimation,
        workedMinutes = this.workedMinutes,
        priority = this.priority,
        statusId = this.statusId,
        categoryIds = this.categoryIds.toMutableSet(),
        assigneeIds = this.assigneeIds.toMutableSet(),
        attachmentIds = this.attachmentIds.toMutableSet(),
        accessRoleId = this.accessRoleId,
        createdBy = this.createdBy,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun TaskJpaEntity.toDomain() =
    Task(
        id = this.id,
        projectId = this.projectId,
        number = this.number,
        name = this.name,
        description = this.description,
        priceEstimation = this.priceEstimation,
        workedMinutes = this.workedMinutes,
        priority = this.priority,
        statusId = this.statusId,
        categoryIds = this.categoryIds.toSet(),
        assigneeIds = this.assigneeIds.toSet(),
        attachmentIds = this.attachmentIds.toSet(),
        accessRoleId = this.accessRoleId,
        createdBy = this.createdBy,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
