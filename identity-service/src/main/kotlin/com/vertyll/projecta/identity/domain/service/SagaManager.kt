package com.vertyll.projecta.identity.domain.service

import com.vertyll.projecta.identity.domain.model.entity.Saga
import com.vertyll.projecta.identity.domain.model.entity.SagaStep
import com.vertyll.projecta.identity.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.identity.domain.model.enums.SagaStatus
import com.vertyll.projecta.identity.domain.model.enums.SagaStepNames
import com.vertyll.projecta.identity.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.identity.domain.model.enums.SagaTypes
import com.vertyll.projecta.identity.domain.repository.SagaRepository
import com.vertyll.projecta.identity.domain.repository.SagaStepRepository
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaTopicNames
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Manages the state of sagas and coordinates compensating transactions.
 */
@Service
class SagaManager(
    private val sagaRepository: SagaRepository,
    private val sagaStepRepository: SagaStepRepository,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val sagaStepDefinitions =
        mapOf(
            SagaTypes.USER_REGISTRATION.value to
                listOf(
                    SagaStepNames.CREATE_USER.value,
                    SagaStepNames.CREATE_USER_EVENT.value,
                    SagaStepNames.CREATE_VERIFICATION_TOKEN.value,
                    SagaStepNames.CREATE_MAIL_EVENT.value,
                ),
            SagaTypes.PASSWORD_RESET.value to
                listOf(
                    SagaStepNames.CREATE_RESET_TOKEN.value,
                    SagaStepNames.CREATE_MAIL_EVENT.value,
                ),
            SagaTypes.EMAIL_VERIFICATION.value to
                listOf(
                    SagaStepNames.CREATE_VERIFICATION_TOKEN.value,
                    SagaStepNames.CREATE_MAIL_EVENT.value,
                ),
            SagaTypes.PASSWORD_CHANGE.value to
                listOf(
                    SagaStepNames.VERIFY_CURRENT_PASSWORD.value,
                    SagaStepNames.CREATE_VERIFICATION_TOKEN.value,
                    SagaStepNames.CREATE_MAIL_EVENT.value,
                    SagaStepNames.UPDATE_PASSWORD.value,
                ),
            SagaTypes.EMAIL_CHANGE.value to
                listOf(
                    SagaStepNames.CREATE_VERIFICATION_TOKEN.value,
                    SagaStepNames.CREATE_MAIL_EVENT.value,
                    SagaStepNames.UPDATE_EMAIL.value,
                ),
        )

    /**
     * Starts a new saga
     * @param sagaType The type of saga to start
     * @param payload Additional data related to the saga
     * @return The created saga instance
     */
    @Transactional
    fun startSaga(
        sagaType: SagaTypes,
        payload: Any,
    ): Saga {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val saga =
            Saga(
                id = UUID.randomUUID().toString(),
                type = sagaType.value,
                status = SagaStatus.STARTED,
                payload = payloadJson,
                startedAt = Instant.now(),
            )

        return sagaRepository.save(saga)
    }

    /**
     * Records a step in a saga
     * @param sagaId The ID of the saga
     * @param stepName The name of the step
     * @param status The status of the step
     * @param payload Additional data related to the step
     * @return The created saga step
     */
    @Transactional
    fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Any? = null,
    ): SagaStep {
        val existingSteps = sagaStepRepository.findBySagaIdAndStepName(sagaId, stepName.value)
        if (existingSteps.isNotEmpty()) {
            val existingStep = existingSteps.first()
            logger.info("Saga step $stepName already exists for saga $sagaId, status: ${existingStep.status}")
            return existingStep
        }

        val payloadJson =
            payload?.let {
                it as? String ?: objectMapper.writeValueAsString(it)
            }

        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        val step =
            SagaStep(
                sagaId = sagaId,
                stepName = stepName.value,
                status = status,
                payload = payloadJson,
                createdAt = Instant.now(),
            )

        val savedStep = sagaStepRepository.save(step)

        if (status == SagaStepStatus.COMPLETED) {
            savedStep.completedAt = Instant.now()
            sagaStepRepository.save(savedStep)
        }

        if (status == SagaStepStatus.FAILED) {
            saga.status = SagaStatus.COMPENSATING
            saga.lastError = "Step $stepName failed"
            sagaRepository.save(saga)

            triggerCompensation(saga)
        } else if (status == SagaStepStatus.COMPLETED) {
            saga.updatedAt = Instant.now()

            if (areAllStepsCompleted(saga)) {
                saga.status = SagaStatus.COMPLETED
                saga.completedAt = Instant.now()
                logger.info("All steps completed for saga ${saga.id}, marking as COMPLETED")
            }

            sagaRepository.save(saga)
        }

        return savedStep
    }

    /**
     * Checks if all expected steps for a saga have been completed
     * @param saga The saga to check
     * @return True if all expected steps are completed, false otherwise
     */
    private fun areAllStepsCompleted(saga: Saga): Boolean {
        val expectedSteps = sagaStepDefinitions[saga.type] ?: return false

        val completedSteps =
            sagaStepRepository.findBySagaIdAndStatus(
                saga.id,
                SagaStepStatus.COMPLETED,
            )

        val completedStepNames = completedSteps.map { it.stepName }

        return expectedSteps.all { expectedStep ->
            completedStepNames.contains(expectedStep)
        }
    }

    /**
     * Marks a saga as completed
     * @param sagaId The ID of the saga to complete
     * @return The updated saga
     */
    @Transactional
    fun completeSaga(sagaId: String): Saga {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        saga.status = SagaStatus.COMPLETED
        saga.completedAt = Instant.now()

        return sagaRepository.save(saga)
    }

    /**
     * Marks a saga as failed and initiates compensation
     * @param sagaId The ID of the saga that failed
     * @param error The error that caused the failure
     * @return The updated saga
     */
    @Transactional
    fun failSaga(
        sagaId: String,
        error: String,
    ): Saga {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        saga.status = SagaStatus.FAILED
        saga.lastError = error
        saga.updatedAt = Instant.now()

        val savedSaga = sagaRepository.save(saga)

        triggerCompensation(savedSaga)

        return savedSaga
    }

    /**
     * Triggers compensation for a failed saga
     * @param saga The saga to compensate
     * @return Unit
     */
    private fun triggerCompensation(saga: Saga) {
        val completedSteps =
            sagaStepRepository
                .findBySagaIdAndStatus(
                    saga.id,
                    SagaStepStatus.COMPLETED,
                ).sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga ${saga.id} with ${completedSteps.size} steps to compensate")

        completedSteps.forEach { step ->
            try {
                logger.info("Compensating step ${step.stepName} (ID: ${step.id}) for saga ${saga.id}")

                when (step.stepName) {
                    SagaStepNames.CREATE_USER.value -> compensateCreateUser(saga.id, step)
                    SagaStepNames.CREATE_VERIFICATION_TOKEN.value -> compensateCreateVerificationToken(saga.id, step)
                    SagaStepNames.UPDATE_PASSWORD.value -> compensateUpdatePassword(saga.id, step)
                    SagaStepNames.UPDATE_EMAIL.value -> compensateUpdateEmail(saga.id, step)
                    else -> logger.warn("No compensation defined for step ${step.stepName}")
                }

                val compensationStep =
                    SagaStep(
                        sagaId = saga.id,
                        stepName = SagaStepNames.compensationNameFromString(step.stepName),
                        status = SagaStepStatus.STARTED,
                        createdAt = Instant.now(),
                    )
                val savedCompensationStep = sagaStepRepository.save(compensationStep)

                step.compensationStepId = savedCompensationStep.id
                sagaStepRepository.save(step)
            } catch (e: Exception) {
                logger.error("Failed to create compensation event for step ${step.stepName}: ${e.message}", e)
            }
        }

        saga.status = SagaStatus.COMPENSATING
        sagaRepository.save(saga)
    }

    /**
     * Compensate for creating a user
     * This will delete the user created in the saga step
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateCreateUser(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()

            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload =
                    mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.DELETE_USER.value,
                        "userId" to userId,
                    ),
                sagaId = sagaId,
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for CreateUser: ${e.message}", e)
        }
    }

    /**
     * Compensate for creating a verification token
     * This will delete the verification token created in the saga step
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateCreateVerificationToken(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val tokenId = (payload["tokenId"] as Number).toLong()

            kafkaOutboxProcessor.saveOutboxMessage(
                topic = KafkaTopicNames.SAGA_COMPENSATION,
                key = sagaId,
                payload =
                    mapOf(
                        "sagaId" to sagaId,
                        "stepId" to step.id,
                        "action" to SagaCompensationActions.DELETE_VERIFICATION_TOKEN.value,
                        "tokenId" to tokenId,
                    ),
                sagaId = sagaId,
            )
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for CreateVerificationToken: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating a password
     * This will revert the password update made in the saga step
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateUpdatePassword(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()
            val originalPasswordHash = payload["originalPasswordHash"]?.toString()

            if (originalPasswordHash != null) {
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload =
                        mapOf(
                            "sagaId" to sagaId,
                            "stepId" to step.id,
                            "action" to SagaCompensationActions.REVERT_PASSWORD_UPDATE.value,
                            "userId" to userId,
                            "originalPasswordHash" to originalPasswordHash,
                        ),
                    sagaId = sagaId,
                )
            } else {
                logger.warn("No original password hash available for compensating password update for user $userId")
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdatePassword: ${e.message}", e)
        }
    }

    /**
     * Compensate for updating an email
     * This will revert the email update made in the saga step
     * @param sagaId The ID of the saga
     * @param step The saga step that needs compensation
     * @return Unit
     */
    private fun compensateUpdateEmail(
        sagaId: String,
        step: SagaStep,
    ) {
        try {
            val payload = objectMapper.readValue(step.payload, Map::class.java)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()
            val originalEmail = payload["originalEmail"]?.toString()

            if (originalEmail != null) {
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.SAGA_COMPENSATION,
                    key = sagaId,
                    payload =
                        mapOf(
                            "sagaId" to sagaId,
                            "stepId" to step.id,
                            "action" to SagaCompensationActions.REVERT_EMAIL_UPDATE.value,
                            "userId" to userId,
                            "originalEmail" to originalEmail,
                        ),
                    sagaId = sagaId,
                )
            } else {
                logger.warn("No original email available for compensating email update for user $userId")
            }
        } catch (e: Exception) {
            logger.error("Failed to create compensation event for UpdateEmail: ${e.message}", e)
        }
    }
}
