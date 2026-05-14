package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.port.out.IdentityProviderPort
import com.vertyll.veds.iam.application.saga.model.SagaCompensationActions
import com.vertyll.veds.iam.domain.repository.UserRepository
import com.vertyll.veds.iam.domain.repository.VerificationTokenRepository
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaStepJpaRepository
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
    private val identityProvider: IdentityProviderPort,
    sagaStepRepository: SagaStepJpaRepository,
    objectMapper: ObjectMapper,
) : BaseSagaCompensationService<SagaStepJpaEntity>(sagaStepRepository, objectMapper) {
    @KafkaListener(topics = [SagaManagerAdapter.SAGA_COMPENSATION_TOPIC])
    override fun handleCompensationEvent(payload: String) = super.handleCompensationEvent(payload)

    override fun createCompensationStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): SagaStepJpaEntity =
        SagaStepJpaEntity(
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
