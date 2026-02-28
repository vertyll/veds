package com.vertyll.projecta.identity.domain.repository

import com.vertyll.projecta.identity.domain.model.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    @EntityGraph(attributePaths = ["roles"])
    override fun findById(id: Long): Optional<User>

    @EntityGraph(attributePaths = ["roles"])
    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean
}
