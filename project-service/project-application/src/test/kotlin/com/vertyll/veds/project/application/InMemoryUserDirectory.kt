package com.vertyll.veds.project.application

import com.vertyll.veds.project.domain.model.UserRef
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import java.util.UUID

internal class InMemoryUserDirectory : UserDirectoryRepository {
    val stored = linkedMapOf<UUID, UserRef>()

    fun given(vararg users: UserRef) = users.forEach { stored[it.userId] = it }

    override fun save(user: UserRef) = user.also { stored[it.userId] = it }

    override fun findById(userId: UUID) = stored[userId]

    override fun findAllByIds(userIds: Collection<UUID>) = userIds.mapNotNull { stored[it] }

    override fun findByEmail(email: String) = stored.values.firstOrNull { it.email == email }
}
