package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.port.inbound.AuthCompensationUseCase
import com.vertyll.veds.iam.application.port.out.IdentityProviderPort
import com.vertyll.veds.iam.application.saga.model.SagaCompensationActions
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
internal class AuthCompensationService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val identityProvider: IdentityProviderPort,
) : AuthCompensationUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun compensate(
        action: String,
        event: Map<String, Any?>,
    ) {
        when (action) {
            SagaCompensationActions.DELETE_USER.value -> compensateCreateUser(event)
            SagaCompensationActions.DELETE_VERIFICATION_TOKEN.value -> compensateCreateVerificationToken(event)
            SagaCompensationActions.REVERT_PASSWORD_UPDATE.value -> compensateUpdatePassword(event)
            SagaCompensationActions.REVERT_EMAIL_UPDATE.value -> compensateUpdateEmail(event)
            else -> logger.warn("Unknown compensation action: $action")
        }
    }

    private fun compensateCreateUser(event: Map<String, Any?>) {
        try {
            val userId = (event["userId"] as Number).toLong()
            userRepository.findById(userId)?.let {
                logger.info("Compensating CreateUser step: Deleting user with ID $userId")
                userRepository.deleteById(userId)
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate CreateUser step: ${e.message}", e)
            throw e
        }
    }

    private fun compensateCreateVerificationToken(event: Map<String, Any?>) {
        try {
            val tokenId = (event["tokenId"] as Number).toLong()
            verificationTokenRepository.findById(tokenId)?.let {
                logger.info("Compensating CreateVerificationToken step: Deleting token with ID $tokenId")
                verificationTokenRepository.deleteById(tokenId)
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate CreateVerificationToken step: ${e.message}", e)
            throw e
        }
    }

    private fun compensateUpdatePassword(event: Map<String, Any?>) {
        try {
            val userId = (event["userId"] as Number).toLong()
            logger.warn(
                "Cannot revert password change for user $userId — passwords are managed by Keycloak. " +
                    "Manual intervention may be required.",
            )
        } catch (e: Exception) {
            logger.error("Failed to compensate UpdatePassword step: ${e.message}", e)
            throw e
        }
    }

    private fun compensateUpdateEmail(event: Map<String, Any?>) {
        try {
            val userId = (event["userId"] as Number).toLong()
            val originalEmail = event["originalEmail"]?.toString()

            if (originalEmail != null) {
                userRepository.findById(userId)?.let { user ->
                    logger.info("Compensating UpdateEmail step: Reverting email for user ID $userId to $originalEmail")
                    user.keycloakId?.let { identityProvider.updateEmail(it, originalEmail) }
                    userRepository.save(user.withEmail(originalEmail))
                }
            } else {
                logger.warn("No original email available for compensating email update for user $userId")
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate UpdateEmail step: ${e.message}", e)
            throw e
        }
    }
}
