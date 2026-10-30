package com.vertyll.veds.iam.infrastructure

import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/** Stands in for Keycloak, which a test does not run, and keeps what it was asked to do. */
class RecordingIdentityProvider : IdentityProviderPort {
    val calls = CopyOnWriteArrayList<String>()

    override fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String,
    ): UUID {
        calls += "createUser $email"
        return UUID.randomUUID()
    }

    override fun enableUser(keycloakId: UUID) {
        calls += "enableUser $keycloakId"
    }

    override fun resetPassword(
        keycloakId: UUID,
        newPassword: String,
    ) {
        calls += "resetPassword $keycloakId"
    }

    override fun updateEmail(
        keycloakId: UUID,
        newEmail: String,
    ) {
        calls += "updateEmail $newEmail"
    }

    override fun createRole(
        roleName: String,
        description: String?,
    ) {
        calls += "createRole $roleName"
    }

    override fun deleteRole(roleName: String) {
        calls += "deleteRole $roleName"
    }

    override fun assignRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        calls += "assignRole $roleName"
    }

    override fun removeRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        calls += "removeRole $roleName"
    }

    override fun credentialTypes(keycloakId: UUID): Set<String> = emptySet()

    override fun setLanguage(
        keycloakId: UUID,
        language: String,
    ) {
        calls += "setLanguage $language"
    }

    override fun credentialId(
        keycloakId: UUID,
        credentialType: String,
    ): String? = null
}
