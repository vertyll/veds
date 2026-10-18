package com.vertyll.veds.project.infrastructure.persistence.repository

import com.vertyll.veds.project.infrastructure.persistence.entity.UserRefJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
internal interface UserRefJpaRepository : JpaRepository<UserRefJpaEntity, UUID> {
    fun findByEmailIgnoreCase(email: String): Optional<UserRefJpaEntity>
}
