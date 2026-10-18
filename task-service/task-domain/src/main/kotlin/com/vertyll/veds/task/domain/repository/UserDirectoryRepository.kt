package com.vertyll.veds.task.domain.repository

import com.vertyll.veds.task.domain.model.UserRef
import java.util.UUID

interface UserDirectoryRepository {
    fun save(user: UserRef): UserRef

    fun findById(userId: UUID): UserRef?

    fun findAllByIds(userIds: Collection<UUID>): List<UserRef>
}
