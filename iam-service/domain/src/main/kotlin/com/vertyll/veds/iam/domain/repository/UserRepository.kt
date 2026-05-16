package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.PageRequest
import com.vertyll.veds.iam.domain.model.PageResult
import com.vertyll.veds.iam.domain.model.User
import java.util.UUID

interface UserRepository {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun findByKeycloakId(keycloakId: UUID): User?

    fun existsByEmail(email: String): Boolean

    fun findAll(pageRequest: PageRequest): PageResult<User>

    fun deleteById(id: Long)
}
