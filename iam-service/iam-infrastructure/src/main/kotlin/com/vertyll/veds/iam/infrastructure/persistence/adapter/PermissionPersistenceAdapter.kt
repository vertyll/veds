package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.model.RoleScope
import com.vertyll.veds.iam.domain.repository.PermissionRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.PermissionJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.PermissionJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class PermissionPersistenceAdapter(
    private val repository: PermissionJpaRepository,
) : PermissionRepository {
    override fun save(permission: Permission): Permission = repository.save(permission.toJpaEntity()).toDomain()

    override fun findById(id: Long): Permission? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByName(name: String): Permission? = repository.findByName(name).orElse(null)?.toDomain()

    override fun existsByName(name: String): Boolean = repository.existsByName(name)

    override fun findAll(): List<Permission> = repository.findAll().map { it.toDomain() }

    override fun findByModule(module: String): List<Permission> = repository.findByModule(module).map { it.toDomain() }

    override fun findAllByNames(names: Collection<String>): List<Permission> = repository.findByNameIn(names).map { it.toDomain() }

    override fun delete(permission: Permission) {
        repository.deleteById(permission.id ?: error("cannot delete an unsaved permission '${permission.name}'"))
    }
}

private fun Permission.toJpaEntity() =
    PermissionJpaEntity(
        id = this.id,
        name = this.name,
        module = this.module,
        scope = this.scope.name,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )

internal fun PermissionJpaEntity.toDomain() =
    Permission(
        id = this.id,
        name = this.name,
        module = this.module,
        scope = RoleScope.fromString(this.scope) ?: error("permission '${this.name}' carries an unknown scope '${this.scope}'"),
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
    )
