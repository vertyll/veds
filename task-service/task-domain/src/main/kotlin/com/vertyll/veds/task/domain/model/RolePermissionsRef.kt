package com.vertyll.veds.task.domain.model

import java.time.Instant

data class RolePermissionsRef(
    val roleName: String,
    val permissions: Set<String> = emptySet(),
    val unrestricted: Boolean = false,
    val updatedAt: Instant = Instant.now(),
) {
    fun grants(permission: TaskPermission): Boolean = unrestricted || permission.name in permissions
}
