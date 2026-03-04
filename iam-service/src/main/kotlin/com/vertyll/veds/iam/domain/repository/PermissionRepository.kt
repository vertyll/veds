package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByName(name: String): Optional<Permission>

    fun existsByName(name: String): Boolean
}
