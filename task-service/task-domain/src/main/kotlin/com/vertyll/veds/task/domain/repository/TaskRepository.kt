package com.vertyll.veds.task.domain.repository

import com.vertyll.veds.task.domain.model.PageRequest
import com.vertyll.veds.task.domain.model.PageResult
import com.vertyll.veds.task.domain.model.Task
import com.vertyll.veds.task.domain.model.TaskSearchCriteria
import java.util.UUID

interface TaskRepository {
    fun save(task: Task): Task

    fun saveAll(tasks: Collection<Task>): List<Task>

    fun findById(id: UUID): Task?

    fun findAllByIds(ids: Collection<UUID>): List<Task>

    fun search(
        criteria: TaskSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<Task>

    fun findAllByProjectId(projectId: UUID): List<Task>

    fun findAllByCategoryId(categoryId: UUID): List<Task>

    fun findAllByStatusId(statusId: UUID): List<Task>

    fun findAllByAttachmentId(attachmentId: UUID): List<Task>

    fun highestNumberIn(projectId: UUID): Int

    fun delete(id: UUID)
}
