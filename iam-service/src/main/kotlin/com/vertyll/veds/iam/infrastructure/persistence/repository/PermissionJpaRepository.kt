package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.PermissionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
internal interface PermissionJpaRepository : JpaRepository<PermissionJpaEntity, Long> {
    fun findByName(name: String): Optional<PermissionJpaEntity>

    fun existsByName(name: String): Boolean
}
