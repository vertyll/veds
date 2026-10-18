package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import java.util.UUID

internal class InMemoryCategoryRepository : ProjectCategoryRepository {
    val stored = linkedMapOf<UUID, ProjectCategory>()

    fun given(vararg categories: ProjectCategory) = categories.forEach { stored[it.id] = it }

    override fun save(category: ProjectCategory) = category.also { stored[it.id] = it }

    override fun findById(id: UUID) = stored[id]

    override fun findAllByProjectId(projectId: UUID) = stored.values.filter { it.projectId == projectId }

    override fun delete(id: UUID) {
        stored.remove(id)
    }
}
