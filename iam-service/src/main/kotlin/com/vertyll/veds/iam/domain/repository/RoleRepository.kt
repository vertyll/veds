package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Optional<Role>

    fun existsByName(name: String): Boolean
}
