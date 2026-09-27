package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import java.util.UUID

internal class InMemoryStatusRepository : ProjectStatusRepository {
    val stored = linkedMapOf<UUID, ProjectStatus>()

    fun given(vararg statuses: ProjectStatus) = statuses.forEach { stored[it.id] = it }

    override fun save(status: ProjectStatus) = status.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}
