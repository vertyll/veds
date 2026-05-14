package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.PermissionJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.PermissionJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PermissionPersistenceAdapter(
    private val repository: PermissionJpaRepository,
) : PermissionRepository {
    override fun save(permission: Permission): Permission = repository.save(permission.toJpaEntity()).toDomain()

    override fun findById(id: Long): Permission? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByName(name: String): Permission? = repository.findByName(name).orElse(null)?.toDomain()

    override fun existsByName(name: String): Boolean = repository.existsByName(name)

    override fun findAll(): List<Permission> = repository.findAll().map { it.toDomain() }
}

internal fun Permission.toJpaEntity() =
    PermissionJpaEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun PermissionJpaEntity.toDomain() =
    Permission(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
