package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.ProjectCategory
import com.vertyll.veds.project.domain.repository.ProjectCategoryRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectCategoryJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.TranslationEmbeddable
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectCategoryJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ProjectCategoryPersistenceAdapter(
    private val repository: ProjectCategoryJpaRepository,
) : ProjectCategoryRepository {
    override fun save(category: ProjectCategory): ProjectCategory = repository.save(category.toJpaEntity()).toDomain()

    override fun findById(id: UUID): ProjectCategory? = repository.findByIdOrNull(id)?.toDomain()

    override fun findAllByProjectId(projectId: UUID): List<ProjectCategory> = repository.findAllByProjectId(projectId).map { it.toDomain() }

    override fun delete(id: UUID) = repository.deleteById(id)
}

private fun ProjectCategory.toJpaEntity() =
    ProjectCategoryJpaEntity(
        id = this.id,
        projectId = this.projectId,
        color = this.color,
        isActive = this.isActive,
        translations = this.translations.map { TranslationEmbeddable.from(it) }.toMutableSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun ProjectCategoryJpaEntity.toDomain() =
    ProjectCategory(
        id = this.id,
        projectId = this.projectId,
        color = this.color,
        isActive = this.isActive,
        translations = this.translations.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
