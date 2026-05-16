package com.vertyll.veds.iam.application.port.out

import com.vertyll.veds.iam.domain.model.RoleType
import java.util.UUID

interface IdentityProviderPort {
    fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String = RoleType.USER.name,
    ): UUID

    fun enableUser(keycloakId: UUID)

    fun resetPassword(
        keycloakId: UUID,
        newPassword: String,
    )

    fun updateEmail(
        keycloakId: UUID,
        newEmail: String,
    )

    fun assignRole(
        keycloakUserId: String,
        roleName: String,
    )

    fun removeRole(
        keycloakUserId: String,
        roleName: String,
    )

    fun validatePassword(
        email: String,
        password: String,
    ): Boolean
}
