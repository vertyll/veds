package com.vertyll.veds.iam.application

import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import java.util.UUID

internal class FakeIdentityProvider : IdentityProviderPort {
    val calls = mutableListOf<String>()
    var createUserFails: Exception? = null

    override fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String,
    ): UUID {
        createUserFails?.let { throw it }
        calls += "createUser($email,$roleName)"
        return UUID.nameUUIDFromBytes(email.toByteArray())
    }

    override fun enableUser(keycloakId: UUID) {
        calls += "enableUser($keycloakId)"
    }

    override fun resetPassword(
        keycloakId: UUID,
        newPassword: String,
    ) {
        calls += "resetPassword($keycloakId)"
    }

    override fun updateEmail(
        keycloakId: UUID,
        newEmail: String,
    ) {
        calls += "updateEmail($keycloakId,$newEmail)"
    }

    override fun createRole(
        roleName: String,
        description: String?,
    ) {
        calls += "createRole($roleName)"
    }

    override fun deleteRole(roleName: String) {
        calls += "deleteRole($roleName)"
    }

    override fun assignRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        calls += "assignRole($keycloakUserId,$roleName)"
    }

    override fun removeRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        calls += "removeRole($keycloakUserId,$roleName)"
    }

    override fun credentialTypes(keycloakId: UUID) = setOf("password")

    override fun removeCredential(
        keycloakId: UUID,
        credentialType: String,
    ) {
        calls += "removeCredential($keycloakId,$credentialType)"
    }
}
