package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @EntityGraph(attributePaths = ["roles", "permissions"])
    override fun findById(id: Long): Optional<User>

    @EntityGraph(attributePaths = ["roles", "permissions"])
    fun findByEmail(email: String): Optional<User>

    @EntityGraph(attributePaths = ["roles", "permissions"])
    fun findByKeycloakId(keycloakId: UUID): Optional<User>

    fun existsByEmail(email: String): Boolean
}
