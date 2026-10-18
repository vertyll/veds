package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import java.util.UUID

interface ProjectTypeRepository {
    fun save(projectType: ProjectType): ProjectType

    fun findById(id: UUID): ProjectType?

    fun findByCode(code: ProjectTypeCode): ProjectType?

    fun existsByCode(code: ProjectTypeCode): Boolean

    fun findAll(): List<ProjectType>
}
