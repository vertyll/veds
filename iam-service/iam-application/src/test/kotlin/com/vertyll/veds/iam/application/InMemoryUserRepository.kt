package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.User
import com.vertyll.veds.iam.domain.repository.UserRepository
import java.util.UUID

internal class InMemoryUserRepository : UserRepository {
    val stored = linkedMapOf<Long, User>()
    private var nextId = 1L

    fun given(vararg users: User) = users.forEach { stored[it.id!!] = it }

    override fun save(user: User): User {
        val withId = user.id?.let { user } ?: user.copy(id = nextId++)
        stored[withId.id!!] = withId
        return withId
    }

    override fun findById(id: Long) = stored[id]

    override fun findByEmail(email: String) = stored.values.firstOrNull { it.email == email }

    override fun findByKeycloakId(keycloakId: UUID) = stored.values.firstOrNull { it.keycloakId == keycloakId }

    override fun existsByEmail(email: String) = findByEmail(email) != null

    override fun findAll(pageRequest: PageRequest) =
        PageResult(content = stored.values.toList(), page = 0, size = stored.size, totalElements = stored.size.toLong())

    override fun search(
        term: String,
        pageRequest: PageRequest,
    ): PageResult<User> {
        val matching =
            stored.values.filter {
                it.email.contains(term, ignoreCase = true) ||
                    it.firstName.contains(term, ignoreCase = true) ||
                    it.lastName.contains(term, ignoreCase = true)
            }
        return PageResult(content = matching, page = 0, size = matching.size, totalElements = matching.size.toLong())
    }

    override fun countByRole(roleId: Long) = stored.values.count { user -> user.roles.any { it.id == roleId } }.toLong()

    override fun deleteById(id: Long) {
        stored.remove(id)
    }
}
