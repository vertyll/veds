package com.vertyll.veds.iam.domain.model

import java.time.Instant

data class Permission(
    val id: Long? = null,
    val name: String,
    val module: String = DEFAULT_MODULE,
    val scope: RoleScope = RoleScope.PROJECT,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    companion object {
        const val DEFAULT_MODULE: String = "admin"

        fun create(
            name: String,
            module: String = DEFAULT_MODULE,
            scope: RoleScope = RoleScope.PROJECT,
            description: String? = null,
        ): Permission =
            Permission(
                name = name,
                module = module,
                scope = scope,
                description = description,
            )
    }
}
