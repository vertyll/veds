package com.vertyll.veds.iam.infrastructure.service

import com.vertyll.veds.iam.domain.model.entity.SagaStep
import com.vertyll.veds.iam.domain.model.enums.SagaCompensationActions
import com.vertyll.veds.iam.domain.repository.SagaStepRepository
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.iam.domain.service.KeycloakAdminService
import com.vertyll.veds.iam.domain.service.SagaManager
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaCompensationService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SagaCompensationService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val keycloakAdminService: KeycloakAdminService,
    sagaStepRepository: SagaStepRepository,
    objectMapper: ObjectMapper,
) : BaseSagaCompensationService<SagaStep>(sagaStepRepository, objectMapper) {
    @KafkaListener(topics = [SagaManager.SAGA_COMPENSATION_TOPIC])
    override fun handleCompensationEvent(payload: String) = super.handleCompensationEvent(payload)

    override fun createCompensationStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): SagaStep =
        SagaStep(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            createdAt = createdAt,
            completedAt = completedAt,
            compensationStepId = compensationStepId,
        )

    override fun processCompensation(
        sagaId: String,
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

    /**
     * Compensate for creating a user by deleting it
     */
    private fun compensateCreateUser(event: Map<String, Any?>) {
        try {
            val userId = (event["userId"] as Number).toLong()

            userRepository.findById(userId).ifPresent { user ->
                logger.info("Compensating CreateUser step: Deleting user with ID $userId")
                userRepository.delete(user)
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate CreateUser step: ${e.message}", e)
            throw e
        }
    }

    /**
     * Compensate for creating a verification token by deleting it
     */
    private fun compensateCreateVerificationToken(event: Map<String, Any?>) {
        try {
            val tokenId = (event["tokenId"] as Number).toLong()

            verificationTokenRepository.findById(tokenId).ifPresent { token ->
                logger.info("Compensating CreateVerificationToken step: Deleting token with ID $tokenId")
                verificationTokenRepository.delete(token)
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate CreateVerificationToken step: ${e.message}", e)
            throw e
        }
    }

    /**
     * Compensate for updating a password — passwords are now in Keycloak,
     * so local password revert is no longer possible. Log warning only.
     */
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

    /**
     * Compensate for updating an email by reverting it in both local DB and Keycloak
     */
    private fun compensateUpdateEmail(event: Map<String, Any?>) {
        try {
            val userId = (event["userId"] as Number).toLong()
            val originalEmail = event["originalEmail"]?.toString()

            if (originalEmail != null) {
                userRepository.findById(userId).ifPresent { user ->
                    logger.info("Compensating UpdateEmail step: Reverting email for user ID $userId to $originalEmail")
                    user.keycloakId?.let { keycloakAdminService.updateEmail(it, originalEmail) }
                    user.setEmail(originalEmail)
                    userRepository.save(user)
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
