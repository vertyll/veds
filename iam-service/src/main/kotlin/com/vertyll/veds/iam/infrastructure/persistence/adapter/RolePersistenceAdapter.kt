package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.repository.RoleRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.RoleJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class RolePersistenceAdapter(
    private val repository: RoleJpaRepository,
) : RoleRepository {
    override fun save(role: Role): Role = repository.save(role.toJpaEntity()).toDomain()

    override fun findById(id: Long): Role? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByName(name: String): Role? = repository.findByName(name).orElse(null)?.toDomain()

    override fun existsByName(name: String): Boolean = repository.existsByName(name)

    override fun findAll(): List<Role> = repository.findAll().map { it.toDomain() }
}

internal fun Role.toJpaEntity() =
    RoleJpaEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun RoleJpaEntity.toDomain() =
    Role(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
