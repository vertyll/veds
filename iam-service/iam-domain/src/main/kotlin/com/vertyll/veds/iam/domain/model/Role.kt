package com.vertyll.veds.iam.domain.model

import java.time.Instant

data class Role(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val permissions: Set<String> = emptySet(),
    val unrestricted: Boolean = false,
    val scope: RoleScope = RoleScope.GLOBAL,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    val storedId: Long
        get() = checkNotNull(id) { "role '$name' has no id" }

    fun grants(permissionName: String): Boolean = unrestricted || permissionName in permissions

    fun withPermissions(granted: Set<String>): Role = copy(permissions = granted, updatedAt = Instant.now())

    fun withDescription(value: String?): Role = copy(description = value, updatedAt = Instant.now())

    fun withPermission(permissionName: String): Role {
        if (permissionName in permissions) return this
        return copy(permissions = permissions + permissionName, updatedAt = Instant.now())
    }

    fun withoutPermission(permissionName: String): Role {
        if (permissionName !in permissions) return this
        return copy(permissions = permissions - permissionName, updatedAt = Instant.now())
    }

    companion object {
        fun create(
            name: String,
            description: String? = null,
            permissions: Set<String> = emptySet(),
            unrestricted: Boolean = false,
            scope: RoleScope = RoleScope.GLOBAL,
        ): Role =
            Role(
                name = name,
                description = description,
                permissions = permissions,
                unrestricted = unrestricted,
                scope = scope,
            )
    }
}
