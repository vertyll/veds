package com.vertyll.veds.task.domain.repository

import com.vertyll.veds.task.domain.model.RolePermissionsRef

interface RolePermissionsRepository {
    fun save(role: RolePermissionsRef): RolePermissionsRef

    fun findByName(roleName: String): RolePermissionsRef?

    fun findAll(): List<RolePermissionsRef>

    fun deleteByName(roleName: String)
}
