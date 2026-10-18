package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.ProjectRole
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import com.vertyll.veds.project.domain.repository.ProjectRoleRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.ProjectRoleJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.entity.TranslationEmbeddable
import com.vertyll.veds.project.infrastructure.persistence.repository.ProjectRoleJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ProjectRolePersistenceAdapter(
    private val repository: ProjectRoleJpaRepository,
) : ProjectRoleRepository {
    override fun save(role: ProjectRole): ProjectRole = repository.save(role.toJpaEntity()).toDomain()

    override fun findById(id: UUID): ProjectRole? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByCode(code: ProjectRoleCode): ProjectRole? = repository.findByCode(code.value).orElse(null)?.toDomain()

    override fun existsByCode(code: ProjectRoleCode): Boolean = repository.existsByCode(code.value)

    override fun findAll(): List<ProjectRole> = repository.findAll().map { it.toDomain() }
}

private fun ProjectRole.toJpaEntity() =
    ProjectRoleJpaEntity(
        id = this.id,
        code = this.code.value,
        permissions = this.permissions.toMutableSet(),
        unrestricted = this.unrestricted,
        isActive = this.isActive,
        translations = this.translations.map { TranslationEmbeddable.from(it) }.toMutableSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun ProjectRoleJpaEntity.toDomain() =
    ProjectRole(
        id = this.id,
        code = ProjectRoleCode(this.code),
        permissions = this.permissions.toSet(),
        unrestricted = this.unrestricted,
        isActive = this.isActive,
        translations = this.translations.map { it.toDomain() }.toSet(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
