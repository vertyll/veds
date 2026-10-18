package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.ProjectCategory
import java.util.UUID

interface ProjectCategoryRepository {
    fun save(category: ProjectCategory): ProjectCategory

    fun findById(id: UUID): ProjectCategory?

    fun findAllByProjectId(projectId: UUID): List<ProjectCategory>

    fun delete(id: UUID)
}
