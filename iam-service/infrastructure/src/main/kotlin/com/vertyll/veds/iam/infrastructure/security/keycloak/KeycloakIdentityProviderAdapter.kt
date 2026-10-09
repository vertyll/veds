package com.vertyll.veds.iam.infrastructure.security.keycloak

import com.vertyll.veds.iam.application.port.outbound.IdentityProviderPort
import com.vertyll.veds.iam.domain.error.IamError
import com.vertyll.veds.shared.web.config.SharedKeycloakProperties
import com.vertyll.veds.sharederror.ApiException
import jakarta.ws.rs.core.Response
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class KeycloakIdentityProviderAdapter(
    private val sharedConfig: SharedKeycloakProperties,
) : IdentityProviderPort {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val keycloak by lazy {
        KeycloakBuilder
            .builder()
            .serverUrl(sharedConfig.serverUrl)
            .realm(sharedConfig.realm)
            .clientId(sharedConfig.adminClientId)
            .clientSecret(sharedConfig.adminClientSecret)
            .grantType("client_credentials")
            .build()
    }

    private val realmResource get() = keycloak.realm(sharedConfig.realm)
    private val usersResource get() = realmResource.users()

    override fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String,
    ): UUID {
        val userRepresentation =
            UserRepresentation().apply {
                this.username = email
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                this.isEnabled = false
                this.isEmailVerified = false
            }

        val credential =
            CredentialRepresentation().apply {
                this.type = CredentialRepresentation.PASSWORD
                this.value = password
                this.isTemporary = false
            }
        userRepresentation.credentials = listOf(credential)

        val response: Response = usersResource.create(userRepresentation)

        return when (response.status) {
            HttpStatus.CREATED.value() -> {
                val location =
                    response.location?.path
                        ?: throw ApiException(
                            IamError.IDENTITY_PROVIDER_FAILED,
                            mapOf("reason" to "created without a Location header", "email" to email),
                        )
                val keycloakUserId = UUID.fromString(location.substringAfterLast("/"))
                logger.info("Created Keycloak user: {} with id: {}", email, keycloakUserId)
                assignRole(keycloakUserId.toString(), roleName)
                keycloakUserId
            }

            HttpStatus.CONFLICT.value() -> {
                logger.warn("User already exists in Keycloak: {}", email)
                throw ApiException(IamError.USER_ALREADY_EXISTS, mapOf("email" to email))
            }

            else -> {
                logger.error("Failed to create Keycloak user: {} - status: {}", email, response.status)
                throw ApiException(
                    IamError.IDENTITY_PROVIDER_FAILED,
                    mapOf("status" to response.status),
                )
            }
        }
    }

    override fun enableUser(keycloakId: UUID) {
        val userResource = usersResource[keycloakId.toString()]
        val userRepresentation = userResource.toRepresentation()
        userRepresentation.isEnabled = true
        userRepresentation.isEmailVerified = true
        userResource.update(userRepresentation)
        logger.info("Enabled Keycloak user: {}", keycloakId)
    }

    override fun resetPassword(
        keycloakId: UUID,
        newPassword: String,
    ) {
        val credential =
            CredentialRepresentation().apply {
                type = CredentialRepresentation.PASSWORD
                value = newPassword
                isTemporary = false
            }
        usersResource[keycloakId.toString()].resetPassword(credential)
        logger.info("Reset password for Keycloak user: {}", keycloakId)
    }

    override fun updateEmail(
        keycloakId: UUID,
        newEmail: String,
    ) {
        val userResource = usersResource[keycloakId.toString()]
        val userRepresentation = userResource.toRepresentation()
        userRepresentation.email = newEmail
        userRepresentation.username = newEmail
        userResource.update(userRepresentation)
        logger.info("Updated email for Keycloak user: {} to {}", keycloakId, newEmail)
    }

    override fun createRole(
        roleName: String,
        description: String?,
    ) {
        if (realmResource.roles().list(roleName, true).any { it.name == roleName }) {
            logger.debug("Realm role already present, leaving it alone: {}", roleName)
            return
        }

        realmResource.roles().create(
            RoleRepresentation().apply {
                name = roleName
                this.description = description
            },
        )
        logger.info("Created realm role: {}", roleName)
    }

    override fun deleteRole(roleName: String) {
        realmResource.roles().deleteRole(roleName)
        logger.info("Deleted realm role: {}", roleName)
    }

    override fun assignRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        val role = realmResource.roles()[roleName].toRepresentation()
        usersResource[keycloakUserId]
            .roles()
            .realmLevel()
            .add(listOf(role))
        logger.info("Assigned role {} to Keycloak user: {}", roleName, keycloakUserId)
    }

    override fun removeRole(
        keycloakUserId: String,
        roleName: String,
    ) {
        val role = realmResource.roles()[roleName].toRepresentation()
        usersResource[keycloakUserId]
            .roles()
            .realmLevel()
            .remove(listOf(role))
        logger.info("Removed role {} from Keycloak user: {}", roleName, keycloakUserId)
    }

    override fun credentialTypes(keycloakId: UUID): Set<String> =
        realmResource
            .users()
            .get(keycloakId.toString())
            .credentials()
            .mapNotNull { it.type?.lowercase() }
            .toSet()

    override fun removeCredential(
        keycloakId: UUID,
        credentialType: String,
    ) {
        val userResource = realmResource.users().get(keycloakId.toString())
        userResource
            .credentials()
            .filter { it.type?.equals(credentialType, ignoreCase = true) == true }
            .forEach { credential -> credential.id?.let { userResource.removeCredential(it) } }
    }
}
