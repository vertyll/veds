package com.vertyll.projecta.identity.infrastructure.service

import com.vertyll.projecta.identity.domain.model.entity.SagaStep
import com.vertyll.projecta.identity.domain.model.enums.SagaStepNames
import com.vertyll.projecta.identity.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.identity.domain.repository.SagaStepRepository
import com.vertyll.projecta.identity.domain.repository.UserRepository
import com.vertyll.projecta.identity.domain.repository.VerificationTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Service that handles compensation actions for the Auth Service
 */
@Service
class SagaCompensationService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val sagaStepRepository: SagaStepRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Listens for compensation events and processes them
     */
    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getSagaCompensationTopic()}"])
    @Transactional
    fun handleCompensationEvent(payload: String) {
        try {
            val event =
                objectMapper.readValue(
                    payload,
                    Map::class.java,
                )
            val sagaId = event["sagaId"] as String
            val stepId = (event["stepId"] as Number).toLong()

            val step =
                sagaStepRepository.findById(stepId).orElse(null) ?: run {
                    logger.error("Cannot find saga step with ID $stepId for compensation")
                    return
                }

            logger.info("Processing compensation for saga $sagaId, step ${step.stepName}")

            when (step.stepName) {
                SagaStepNames.CREATE_USER.value -> compensateCreateUser(step)
                SagaStepNames.CREATE_VERIFICATION_TOKEN.value -> compensateCreateVerificationToken(step)
                SagaStepNames.UPDATE_PASSWORD.value -> compensateUpdatePassword(step)
                SagaStepNames.UPDATE_EMAIL.value -> compensateUpdateEmail(step)
                else -> logger.warn("No compensation handler for step ${step.stepName}")
            }

            val compensationStep =
                SagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.compensationNameFromString(step.stepName),
                    status = SagaStepStatus.COMPENSATED,
                    createdAt = java.time.Instant.now(),
                    completedAt = java.time.Instant.now(),
                    compensationStepId = step.id,
                )
            sagaStepRepository.save(compensationStep)
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }

    /**
     * Compensate for creating a user by deleting it
     */
    @Transactional
    fun compensateCreateUser(step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()

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
    @Transactional
    fun compensateCreateVerificationToken(step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val tokenId = (payload["tokenId"] as Number).toLong()

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
     * Compensate for updating a password by reverting it
     */
    @Transactional
    fun compensateUpdatePassword(step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()
            val originalPasswordHash = payload["originalPasswordHash"]?.toString()

            if (originalPasswordHash != null) {
                userRepository.findById(userId).ifPresent { user ->
                    logger.info("Compensating UpdatePassword step: Reverting password for user ID $userId")
                    user.setPassword(originalPasswordHash)
                    userRepository.save(user)
                }
            } else {
                logger.warn("No original password hash available for compensating password update for user $userId")
            }
        } catch (e: Exception) {
            logger.error("Failed to compensate UpdatePassword step: ${e.message}", e)
            throw e
        }
    }

    /**
     * Compensate for updating an email by reverting it
     */
    @Transactional
    fun compensateUpdateEmail(step: SagaStep) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()
            val originalEmail = payload["originalEmail"]?.toString()

            if (originalEmail != null) {
                userRepository.findById(userId).ifPresent { user ->
                    logger.info("Compensating UpdateEmail step: Reverting email for user ID $userId to $originalEmail")
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
