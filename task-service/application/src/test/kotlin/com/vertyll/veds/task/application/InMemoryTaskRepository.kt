package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import com.vertyll.veds.task.domain.repository.TaskRepository
import java.util.UUID

internal class InMemoryTaskRepository : TaskRepository {
    val stored = linkedMapOf<UUID, Task>()

    fun given(vararg tasks: Task) = tasks.forEach { stored[it.id] = it }

    override fun save(task: Task) = task.also { stored[it.id] = it }

    override fun saveAll(tasks: Collection<Task>) = tasks.map { save(it) }

    override fun highestNumberIn(projectId: UUID) = stored.values.filter { it.projectId == projectId }.maxOfOrNull { it.number } ?: 0

    override fun findById(id: UUID) = stored[id]

    override fun findAllByIds(ids: Collection<UUID>) = ids.mapNotNull { stored[it] }

    override fun search(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun findAllByCategoryId(categoryId: UUID) = stored.values.filter { categoryId in it.categoryIds }

    override fun findAllByStatusId(statusId: UUID) = stored.values.filter { it.statusId == statusId }

    override fun findAllByAttachmentId(attachmentId: UUID) = stored.values.filter { attachmentId in it.attachmentIds }

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}
