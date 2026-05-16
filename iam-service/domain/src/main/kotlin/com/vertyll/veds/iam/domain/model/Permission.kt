package com.vertyll.veds.iam.domain.model

import java.time.Instant

data class Permission(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    companion object {
        fun create(
            name: String,
            description: String? = null,
        ): Permission =
            Permission(
                name = name,
                description = description,
            )
    }
}
