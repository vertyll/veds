package com.vertyll.veds.task.infrastructure.persistence.adapter

import com.vertyll.veds.task.domain.model.UserRef
import com.vertyll.veds.task.domain.repository.UserDirectoryRepository
import com.vertyll.veds.task.infrastructure.persistence.entity.UserRefJpaEntity
import com.vertyll.veds.task.infrastructure.persistence.repository.UserRefJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class UserDirectoryPersistenceAdapter(
    private val repository: UserRefJpaRepository,
) : UserDirectoryRepository {
    override fun save(user: UserRef): UserRef =
        repository
            .save(
                UserRefJpaEntity(
                    userId = user.userId,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    avatarFileId = user.avatarFileId,
                    updatedAt = user.updatedAt,
                ),
            ).toDomain()

    override fun findById(userId: UUID): UserRef? = repository.findByIdOrNull(userId)?.toDomain()

    override fun findAllByIds(userIds: Collection<UUID>): List<UserRef> =
        if (userIds.isEmpty()) emptyList() else repository.findAllById(userIds).map { it.toDomain() }
}

private fun UserRefJpaEntity.toDomain() =
    UserRef(
        userId = userId,
        email = email,
        firstName = firstName,
        lastName = lastName,
        avatarFileId = avatarFileId,
        updatedAt = updatedAt,
    )
