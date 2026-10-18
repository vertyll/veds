package com.vertyll.veds.notification.infrastructure.persistence.adapter

import com.vertyll.veds.notification.domain.model.RecipientRef
import com.vertyll.veds.notification.domain.repository.RecipientDirectoryRepository
import com.vertyll.veds.notification.infrastructure.persistence.entity.RecipientRefJpaEntity
import com.vertyll.veds.notification.infrastructure.persistence.repository.RecipientRefJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class RecipientDirectoryPersistenceAdapter(
    private val repository: RecipientRefJpaRepository,
) : RecipientDirectoryRepository {
    override fun save(recipient: RecipientRef): RecipientRef =
        repository
            .save(
                RecipientRefJpaEntity(
                    userId = recipient.userId,
                    email = recipient.email,
                    displayName = recipient.displayName,
                    updatedAt = recipient.updatedAt,
                ),
            ).toDomain()

    override fun findById(userId: UUID): RecipientRef? = repository.findByIdOrNull(userId)?.toDomain()

    override fun findByEmail(email: String): RecipientRef? = repository.findByEmailIgnoreCase(email).orElse(null)?.toDomain()
}

private fun RecipientRefJpaEntity.toDomain() =
    RecipientRef(
        userId = userId,
        email = email,
        displayName = displayName,
        updatedAt = updatedAt,
    )
