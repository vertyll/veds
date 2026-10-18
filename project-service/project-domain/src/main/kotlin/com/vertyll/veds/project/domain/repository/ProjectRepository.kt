package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.PageRequest
import com.vertyll.veds.project.domain.model.PageResult
import com.vertyll.veds.project.domain.model.Project
import com.vertyll.veds.project.domain.model.ProjectSearchCriteria
import java.util.UUID

interface ProjectRepository {
    fun save(project: Project): Project

    fun findById(id: UUID): Project?

    fun findAllByIds(ids: Collection<UUID>): List<Project>

    fun search(
        criteria: ProjectSearchCriteria,
        pageRequest: PageRequest,
    ): PageResult<Project>

    fun existsById(id: UUID): Boolean

    fun delete(id: UUID)
}
