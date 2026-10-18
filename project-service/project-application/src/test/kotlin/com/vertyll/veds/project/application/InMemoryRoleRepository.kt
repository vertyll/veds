package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import java.util.UUID

internal class InMemoryRoleRepository : ProjectRoleRepository {
    val stored = mutableListOf<ProjectRole>()

    fun given(vararg roles: ProjectRole) = roles.forEach { stored += it }

    override fun save(role: ProjectRole) = role.also { stored += it }

    override fun findById(id: UUID) = stored.firstOrNull { it.id == id }

    override fun findByCode(code: ProjectRoleCode) = stored.firstOrNull { it.code == code }

    override fun existsByCode(code: ProjectRoleCode) = findByCode(code) != null

    override fun findAll() = stored.toList()
}
