package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.domain.model.Role
import com.vertyll.veds.iam.domain.repository.RoleRepository

internal class InMemoryRoleRepository : RoleRepository {
    val stored = mutableListOf<Role>()
    private var nextId = 100L

    fun given(vararg roles: Role) =
        roles.forEach {
            stored += it
            nextId = maxOf(nextId, (it.id ?: 0L) + 1)
        }

    override fun save(role: Role): Role {
        val withId = role.id?.let { role } ?: role.copy(id = nextId++)
        stored.removeAll { it.id == withId.id }
        stored += withId
        return withId
    }

    override fun findById(id: Long) = stored.firstOrNull { it.id == id }

    override fun findByName(name: String) = stored.firstOrNull { it.name == name }

    override fun existsByName(name: String) = findByName(name) != null

    override fun findAll() = stored.toList()

    override fun findAllByNames(names: Collection<String>) = stored.filter { it.name in names }

    override fun delete(role: Role) {
        stored.removeAll { it.id == role.id }
    }
}
