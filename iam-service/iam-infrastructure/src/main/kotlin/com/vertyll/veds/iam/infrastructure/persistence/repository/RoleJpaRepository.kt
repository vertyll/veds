package com.vertyll.veds.iam.infrastructure.persistence.repository

import com.vertyll.veds.iam.infrastructure.persistence.entity.RoleJpaEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
internal interface RoleJpaRepository : JpaRepository<RoleJpaEntity, Long> {
    @EntityGraph(attributePaths = ["permissions"])
    fun findByName(name: String): Optional<RoleJpaEntity>

    fun existsByName(name: String): Boolean

    @EntityGraph(attributePaths = ["permissions"])
    fun findByNameIn(names: Collection<String>): List<RoleJpaEntity>

    @EntityGraph(attributePaths = ["permissions"])
    override fun findById(id: Long): Optional<RoleJpaEntity>

    @EntityGraph(attributePaths = ["permissions"])
    override fun findAll(): List<RoleJpaEntity>
}
