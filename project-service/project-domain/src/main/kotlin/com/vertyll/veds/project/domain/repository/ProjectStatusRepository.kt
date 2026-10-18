package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.ProjectStatus
import java.util.UUID

interface ProjectStatusRepository {
    fun save(status: ProjectStatus): ProjectStatus

    fun findById(id: UUID): ProjectStatus?

    fun findAllByProjectId(projectId: UUID): List<ProjectStatus>

    fun delete(id: UUID)
}
