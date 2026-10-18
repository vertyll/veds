package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.RolePermissionsRef
import com.vertyll.veds.task.domain.repository.RolePermissionsRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.RolePermissionsJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.RolePermissionsJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class RolePermissionsPersistenceAdapter(
    private val repository: RolePermissionsJpaRepository,
) : RolePermissionsRepository {
    override fun save(role: RolePermissionsRef): RolePermissionsRef {
        val entity = repository.findByIdOrNull(role.roleName) ?: RolePermissionsJpaEntity(roleName = role.roleName)
        entity.permissions = role.permissions.toMutableSet()
        entity.unrestricted = role.unrestricted
        entity.updatedAt = role.updatedAt
        return repository.save(entity).toDomain()
    }

    override fun findByName(roleName: String): RolePermissionsRef? = repository.findByIdOrNull(roleName)?.toDomain()

    override fun findAll(): List<RolePermissionsRef> = repository.findAll().map { it.toDomain() }

    override fun deleteByName(roleName: String) {
        repository.deleteById(roleName)
    }
}

private fun RolePermissionsJpaEntity.toDomain() =
    RolePermissionsRef(
        roleName = this.roleName,
        permissions = this.permissions.toSet(),
        unrestricted = this.unrestricted,
        updatedAt = this.updatedAt,
    )
