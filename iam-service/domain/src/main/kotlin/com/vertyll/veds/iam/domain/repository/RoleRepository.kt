package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.Role

interface RoleRepository {
    fun save(role: Role): Role

    fun findById(id: Long): Role?

    fun findByName(name: String): Role?

    fun existsByName(name: String): Boolean

    fun findAll(): List<Role>
}
