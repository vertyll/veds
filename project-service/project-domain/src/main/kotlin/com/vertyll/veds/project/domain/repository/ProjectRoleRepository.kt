package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import java.util.UUID

interface ProjectRoleRepository {
    fun save(role: ProjectRole): ProjectRole

    fun findById(id: UUID): ProjectRole?

    fun findByCode(code: ProjectRoleCode): ProjectRole?

    fun existsByCode(code: ProjectRoleCode): Boolean

    fun findAll(): List<ProjectRole>
}
