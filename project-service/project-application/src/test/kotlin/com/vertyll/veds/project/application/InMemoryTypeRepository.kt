package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import java.util.UUID

internal class InMemoryTypeRepository : ProjectTypeRepository {
    val stored = mutableListOf<ProjectType>()

    fun given(vararg types: ProjectType) = types.forEach { stored += it }

    override fun save(projectType: ProjectType) = projectType.also { stored += it }

    override fun findById(id: UUID) = stored.firstOrNull { it.id == id }

    override fun findByCode(code: ProjectTypeCode) = stored.firstOrNull { it.code == code }

    override fun existsByCode(code: ProjectTypeCode) = findByCode(code) != null

    override fun findAll() = stored.toList()
}
