package com.vertyll.veds.notification.infrastructure.persistence.repository

import com.vertyll.veds.notification.infrastructure.persistence.entity.RecipientRefJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
internal interface RecipientRefJpaRepository : JpaRepository<RecipientRefJpaEntity, UUID> {
    fun findByEmailIgnoreCase(email: String): Optional<RecipientRefJpaEntity>
}
