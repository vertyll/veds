package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.PageResult
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import com.vertyll.veds.project.domain.repository.ProjectRepository
import java.util.UUID

internal class InMemoryProjectRepository : ProjectRepository {
    val stored = linkedMapOf<UUID, Project>()

    fun given(vararg projects: Project) = projects.forEach { stored[it.id] = it }

    override fun save(project: Project) = project.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByIds(ids: Collection<UUID>) = ids.mapNotNull { stored[it] }

    override fun search(
        criteria: ProjectSearchCriteria,
        pageRequest: PageRequest,
    ) = PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun existsById(id: UUID) = stored.containsKey(id)

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}
