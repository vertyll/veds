package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.domain.model.Permission
import com.vertyll.veds.iam.domain.repository.PermissionRepository

internal class InMemoryPermissionRepository : PermissionRepository {
    val stored = mutableListOf<Permission>()
    private var nextId = 1L

    fun given(vararg permissions: Permission) =
        permissions.forEach {
            stored += it
            nextId = maxOf(nextId, (it.id ?: 0L) + 1)
        }

    override fun save(permission: Permission): Permission {
        val withId = permission.id?.let { permission } ?: permission.copy(id = nextId++)
        stored.removeAll { it.id == withId.id }
        stored += withId
        return withId
    }

    override fun findById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findByName(name: String) = stored.firstOrNull { it.name == name }

    override fun existsByName(name: String) = findByName(name) != null

    override fun findAll() = stored.toList()

    override fun findByModule(module: String) = stored.filter { it.module == module }

    override fun findAllByNames(names: Collection<String>) = stored.filter { it.name in names }

    override fun delete(permission: Permission) {
        stored.removeAll { it.id == permission.id }
    }
}
