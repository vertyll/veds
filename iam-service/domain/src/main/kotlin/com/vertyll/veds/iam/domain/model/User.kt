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
    val avatarFileId: UUID? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val version: Long? = null,
) {
    init {
        require(email.isNotBlank()) { "user email must not be blank" }
    }

    val permissions: Set<Permission>
        get() = roles.flatMap { it.permissions }.toSet()

    fun withProfile(
        firstName: String,
        lastName: String,
        avatarFileId: UUID?,
        phoneNumber: String?,
        address: String?,
    ): User =
        copy(
            firstName = firstName,
            lastName = lastName,
            avatarFileId = avatarFileId,
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

    companion object {
        fun create(
            keycloakId: UUID,
            email: String,
            firstName: String,
            lastName: String,
            avatarFileId: UUID? = null,
            phoneNumber: String? = null,
            address: String? = null,
        ): User =
            User(
                keycloakId = keycloakId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                avatarFileId = avatarFileId,
                phoneNumber = phoneNumber,
                address = address,
            )
    }
}
