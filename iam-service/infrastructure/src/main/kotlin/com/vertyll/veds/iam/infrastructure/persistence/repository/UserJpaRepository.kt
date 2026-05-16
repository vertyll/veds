package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.UserJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
internal interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    @EntityGraph(attributePaths = ["roles", "permissions"])
    fun findByEmail(email: String): Optional<UserJpaEntity>

    @EntityGraph(attributePaths = ["roles", "permissions"])
    fun findByKeycloakId(keycloakId: UUID): Optional<UserJpaEntity>

    fun existsByEmail(email: String): Boolean

    @EntityGraph(attributePaths = ["roles", "permissions"])
    override fun findAll(pageable: Pageable): Page<UserJpaEntity>

    @EntityGraph(attributePaths = ["roles", "permissions"])
    override fun findById(id: Long): Optional<UserJpaEntity>
}
