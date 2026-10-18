package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.ProjectType
import com.vertyll.veds.project.domain.model.ProjectTypeCode
import com.vertyll.veds.project.domain.repository.ProjectTypeRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectTypeJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.TranslationEmbeddable
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectTypeJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ProjectTypePersistenceAdapter(
    private val repository: ProjectTypeJpaRepository,
) : ProjectTypeRepository {
    override fun save(projectType: ProjectType): ProjectType = repository.save(projectType.toJpaEntity()).toDomain()

    override fun findById(id: UUID): ProjectType? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByCode(code: ProjectTypeCode): ProjectType? = repository.findByCode(code).orElse(null)?.toDomain()

    override fun existsByCode(code: ProjectTypeCode): Boolean = repository.existsByCode(code)

    override fun findAll(): List<ProjectType> = repository.findAll().map { it.toDomain() }
}

private fun ProjectType.toJpaEntity() =
    ProjectTypeJpaEntity(
        id = this.id,
        code = this.code,
        isActive = this.isActive,
        translations = this.translations.map { TranslationEmbeddable.from(it) }.toMutableSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun ProjectTypeJpaEntity.toDomain() =
    ProjectType(
        id = this.id,
        code = this.code,
        isActive = this.isActive,
        translations = this.translations.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
