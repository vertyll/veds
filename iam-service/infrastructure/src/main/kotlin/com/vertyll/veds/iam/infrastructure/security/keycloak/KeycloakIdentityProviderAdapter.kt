package com.vertyll.veds.iam.infrastructure.security.keycloak

import com.vertyll.veds.iam.application.exception.ApiException
import com.vertyll.veds.iam.application.port.out.IdentityProviderPort
import com.vertyll.veds.sharedinfrastructure.config.SharedConfigProperties
import jakarta.ws.rs.core.Response
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class KeycloakIdentityProviderAdapter(
    private val sharedConfig: SharedConfigProperties,
) : IdentityProviderPort {
    private val logger = LoggerFactory.getLogger(javaClass)

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
                val locationHeader = response.location?.path ?: ""
                val keycloakUserId = UUID.fromString(locationHeader.substringAfterLast("/"))
                logger.info("Created Keycloak user: {} with id: {}", email, keycloakUserId)
                assignRole(keycloakUserId.toString(), roleName)
                keycloakUserId
            }
            HttpStatus.CONFLICT.value() -> {
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

    override fun validatePassword(
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
            tokenKeycloak.tokenManager().accessToken
            true
        } catch (_: Exception) {
            logger.debug("Password validation failed for user: {}", email)
            false
        }
}
