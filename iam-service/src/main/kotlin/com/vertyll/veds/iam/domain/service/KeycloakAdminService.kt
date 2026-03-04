package com.vertyll.veds.iam.domain.service

import com.vertyll.veds.iam.infrastructure.exception.ApiException
import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import jakarta.ws.rs.core.Response
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for managing users in Keycloak via the Admin REST API.
 * Uses a dedicated service account client (client_credentials grant) within the application realm.
 */
@Service
class KeycloakAdminService(
    private val sharedConfig: SharedConfigProperties,
) {
    private val logger = LoggerFactory.getLogger(KeycloakAdminService::class.java)

    companion object {
        private const val HTTP_CREATED = 201
        private const val HTTP_CONFLICT = 409
    }

    private val keycloak by lazy {
        KeycloakBuilder
            .builder()
            .serverUrl(sharedConfig.keycloak.serverUrl)
            .realm(sharedConfig.keycloak.realm)
            .clientId(sharedConfig.keycloak.adminClientId)
            .clientSecret(sharedConfig.keycloak.adminClientSecret)
            .grantType("client_credentials")
            .build()
    }

    private val realmResource get() = keycloak.realm(sharedConfig.keycloak.realm)
    private val usersResource get() = realmResource.users()

    /**
     * Creates a new user in Keycloak with the given credentials and assigns the specified role.
     * The user is created as disabled (enabled = false) since account activation is handled separately.
     *
     * @return The Keycloak user UUID (subclaim value)
     */
    fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        roleName: String = "USER",
    ): UUID {
        val userRepresentation =
            UserRepresentation().apply {
                this.username = email
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                this.isEnabled = false // Will be enabled after account activation
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
            HTTP_CREATED -> {
                val locationHeader = response.location?.path ?: ""
                val keycloakUserId = UUID.fromString(locationHeader.substringAfterLast("/"))
                logger.info("Created Keycloak user: {} with id: {}", email, keycloakUserId)

                // Assign role
                assignRole(keycloakUserId.toString(), roleName)

                keycloakUserId
            }
            HTTP_CONFLICT -> {
                logger.warn("User already exists in Keycloak: {}", email)
                throw ApiException("User already exists", HttpStatus.CONFLICT)
            }
            else -> {
                logger.error("Failed to create Keycloak user: {} - status: {}", email, response.status)
                throw ApiException(
                    "Failed to create user in Keycloak (status: ${response.status})",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                )
            }
        }
    }

    /**
     * Enables a user account in Keycloak (used after account activation).
     */
    fun enableUser(keycloakId: UUID) {
        val userResource = usersResource[keycloakId.toString()]
        val userRepresentation = userResource.toRepresentation()
        userRepresentation.isEnabled = true
        userRepresentation.isEmailVerified = true
        userResource.update(userRepresentation)
        logger.info("Enabled Keycloak user: {}", keycloakId)
    }

    /**
     * Resets (sets) a user's password in Keycloak.
     */
    fun resetPassword(
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

    /**
     * Updates the email of a user in Keycloak.
     */
    fun updateEmail(
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

    /**
     * Assigns a realm role to a user in Keycloak.
     */
    fun assignRole(
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

    /**
     * Removes a realm role from a user in Keycloak.
     */
    fun removeRole(
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

    /**
     * Validates a user's current password by attempting to get a token from Keycloak.
     * @return true if the password is valid
     */
    fun validatePassword(
        email: String,
        password: String,
    ): Boolean =
        try {
            val tokenKeycloak =
                KeycloakBuilder
                    .builder()
                    .serverUrl(sharedConfig.keycloak.serverUrl)
                    .realm(sharedConfig.keycloak.realm)
                    .clientId(sharedConfig.keycloak.gatewayClientId)
                    .clientSecret(sharedConfig.keycloak.gatewayClientSecret)
                    .username(email)
                    .password(password)
                    .grantType("password")
                    .build()

            // If we can get a token, the password is correct
            tokenKeycloak.tokenManager().accessToken
            true
        } catch (_: Exception) {
            logger.debug("Password validation failed for user: {}", email)
            false
        }
}
