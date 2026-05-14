package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface UserRepository {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun findByKeycloakId(keycloakId: UUID): User?

    fun existsByEmail(email: String): Boolean

    fun findAll(pageable: Pageable): Page<User>

    fun deleteById(id: Long)
}
