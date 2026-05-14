package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
internal interface RoleJpaRepository : JpaRepository<RoleJpaEntity, Long> {
    fun findByName(name: String): Optional<RoleJpaEntity>

    fun existsByName(name: String): Boolean
}
