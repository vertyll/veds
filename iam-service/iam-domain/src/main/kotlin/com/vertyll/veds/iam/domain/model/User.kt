package com.vertyll.veds.iam.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: Long? = null,
    val keycloakId: UUID? = null,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<String> = emptySet(),
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

    val storedId: Long
        get() = checkNotNull(id) { "user '$keycloakId' has no id" }

    fun permissionsFrom(heldRoles: Collection<Role>): Set<String> =
        heldRoles.filter { it.name in roles }.flatMapTo(mutableSetOf()) { it.permissions }

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

    fun withRole(roleName: String): User {
        if (roleName in roles) return this
        return copy(
            roles = roles + roleName,
            updatedAt = Instant.now(),
        )
    }

    fun withoutRole(roleName: String): User {
        if (roleName !in roles) return this
        return copy(
            roles = roles - roleName,
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
