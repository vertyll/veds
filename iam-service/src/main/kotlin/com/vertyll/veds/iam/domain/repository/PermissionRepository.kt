package com.vertyll.veds.iam.domain.repository

import com.vertyll.veds.iam.domain.model.Permission

interface PermissionRepository {
    fun save(permission: Permission): Permission

    fun findById(id: Long): Permission?

    fun findByName(name: String): Permission?

    fun existsByName(name: String): Boolean

    fun findAll(): List<Permission>
}
