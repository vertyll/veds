package com.vertyll.veds.project.infrastructure.persistence.adapter

import com.vertyll.veds.project.domain.model.UserRef
import com.vertyll.veds.project.domain.repository.UserDirectoryRepository
import com.vertyll.veds.project.infrastructure.persistence.entity.UserRefJpaEntity
import com.vertyll.veds.project.infrastructure.persistence.repository.UserRefJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class UserDirectoryPersistenceAdapter(
    private val repository: UserRefJpaRepository,
) : UserDirectoryRepository {
    override fun save(user: UserRef): UserRef = repository.save(user.toJpaEntity()).toDomain()

    override fun findById(userId: UUID): UserRef? = repository.findByIdOrNull(userId)?.toDomain()

    override fun findAllByIds(userIds: Collection<UUID>): List<UserRef> =
        if (userIds.isEmpty()) emptyList() else repository.findAllById(userIds).map { it.toDomain() }

    override fun findByEmail(email: String): UserRef? = repository.findByEmailIgnoreCase(email).orElse(null)?.toDomain()
}

private fun UserRef.toJpaEntity() =
    UserRefJpaEntity(
        userId = this.userId,
        email = this.email,
        firstName = this.firstName,
        lastName = this.lastName,
        avatarFileId = this.avatarFileId,
        updatedAt = this.updatedAt,
    )

internal fun UserRefJpaEntity.toDomain() =
    UserRef(
        userId = this.userId,
        email = this.email,
        firstName = this.firstName,
        lastName = this.lastName,
        avatarFileId = this.avatarFileId,
        updatedAt = this.updatedAt,
    )
