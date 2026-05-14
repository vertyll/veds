package com.vertyll.veds.iam.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: Long? = null,
    val keycloakId: UUID? = null,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<Role> = emptySet(),
    val permissions: Set<Permission> = emptySet(),
    val profilePicture: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    fun withEmail(newEmail: String): User =
        copy(
            email = newEmail,
            updatedAt = Instant.now(),
        )

    fun withProfile(
        firstName: String,
        lastName: String,
        profilePicture: String?,
        phoneNumber: String?,
        address: String?,
    ): User =
        copy(
            firstName = firstName,
            lastName = lastName,
            profilePicture = profilePicture,
            phoneNumber = phoneNumber,
            address = address,
            updatedAt = Instant.now(),
        )

    fun withRole(role: Role): User {
        if (role.id == null || roles.any { it.id == role.id }) return this
        return copy(
            roles = roles + role,
            updatedAt = Instant.now(),
        )
    }

    fun withoutRole(roleId: Long): User {
        if (roles.none { it.id == roleId }) return this
        return copy(
            roles = roles.filterNot { it.id == roleId }.toSet(),
            updatedAt = Instant.now(),
        )
    }

    fun withPermission(permission: Permission): User {
        if (permission.id == null || permissions.any { it.id == permission.id }) return this
        return copy(
            permissions = permissions + permission,
            updatedAt = Instant.now(),
        )
    }

    fun withoutPermission(permissionId: Long): User {
        if (permissions.none { it.id == permissionId }) return this
        return copy(
            permissions = permissions.filterNot { it.id == permissionId }.toSet(),
            updatedAt = Instant.now(),
        )
    }

    companion object {
        fun create(
            keycloakId: UUID,
            email: String,
            firstName: String,
            lastName: String,
            profilePicture: String? = null,
            phoneNumber: String? = null,
            address: String? = null,
        ): User =
            User(
                keycloakId = keycloakId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                profilePicture = profilePicture,
                phoneNumber = phoneNumber,
                address = address,
            )
    }
}
