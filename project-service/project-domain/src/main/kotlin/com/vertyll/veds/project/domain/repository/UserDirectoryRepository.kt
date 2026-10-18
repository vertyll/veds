package com.vertyll.veds.project.domain.repository

import com.vertyll.veds.project.domain.model.UserRef
import java.util.UUID

interface UserDirectoryRepository {
    fun save(user: UserRef): UserRef

    fun findById(userId: UUID): UserRef?

    fun findAllByIds(userIds: Collection<UUID>): List<UserRef>

    fun findByEmail(email: String): UserRef?
}
