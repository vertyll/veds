package com.vertyll.veds.task.application

import com.vertyll.veds.task.domain.model.UserRef
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import java.util.UUID

internal class InMemoryUserDirectory : UserDirectoryRepository {
    val stored = linkedMapOf<UUID, UserRef>()

    override fun save(user: UserRef) = user.also { stored[it.userId] = it }

    override fun findById(userId: UUID) = stored[userId]

    override fun findAllByIds(userIds: Collection<UUID>) = userIds.mapNotNull { stored[it] }
}
