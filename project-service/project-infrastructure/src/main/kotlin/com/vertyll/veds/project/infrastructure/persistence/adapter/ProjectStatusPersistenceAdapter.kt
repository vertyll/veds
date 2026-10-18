package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.ProjectStatus
import com.vertyll.veds.project.domain.repository.ProjectStatusRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectStatusJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.TranslationEmbeddable
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectStatusJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ProjectStatusPersistenceAdapter(
    private val repository: ProjectStatusJpaRepository,
) : ProjectStatusRepository {
    override fun save(status: ProjectStatus): ProjectStatus = repository.save(status.toJpaEntity()).toDomain()

    override fun findById(id: UUID): ProjectStatus? = repository.findByIdOrNull(id)?.toDomain()

    override fun findAllByProjectId(projectId: UUID): List<ProjectStatus> = repository.findAllByProjectId(projectId).map { it.toDomain() }

    override fun delete(id: UUID) = repository.deleteById(id)
}

private fun ProjectStatus.toJpaEntity() =
    ProjectStatusJpaEntity(
        id = this.id,
        projectId = this.projectId,
        color = this.color,
        isActive = this.isActive,
        translations = this.translations.map { TranslationEmbeddable.from(it) }.toMutableSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun ProjectStatusJpaEntity.toDomain() =
    ProjectStatus(
        id = this.id,
        projectId = this.projectId,
        color = this.color,
        isActive = this.isActive,
        translations = this.translations.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
