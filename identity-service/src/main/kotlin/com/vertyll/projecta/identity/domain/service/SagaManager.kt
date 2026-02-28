package com.vertyll.projecta.identity.domain.service

import com.vertyll.projecta.identity.domain.model.entity.Saga
import com.vertyll.projecta.identity.domain.model.entity.SagaStep
import com.vertyll.projecta.identity.domain.model.enums.SagaCompensationActions
import com.vertyll.projecta.identity.domain.model.enums.SagaStepNames
import com.vertyll.projecta.identity.domain.model.enums.SagaTypes
import com.vertyll.projecta.identity.domain.repository.SagaRepository
import com.vertyll.projecta.identity.domain.repository.SagaStepRepository
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.projecta.sharedinfrastructure.saga.service.BaseSagaManager
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SagaManager(
    sagaRepository: SagaRepository,
    sagaStepRepository: SagaStepRepository,
    kafkaOutboxProcessor: KafkaOutboxProcessor,
    objectMapper: ObjectMapper,
) : BaseSagaManager<Saga, SagaStep>(
        sagaRepository,
        sagaStepRepository,
        kafkaOutboxProcessor,
        objectMapper,
    ) {
    override fun getSagaStepDefinitions(): Map<String, List<String>> =
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

    override fun createSagaEntity(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): Saga =
        Saga(
            id = id,
            type = type,
            status = status,
            payload = payload,
            startedAt = startedAt,
        )

    override fun createSagaStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): SagaStep =
        SagaStep(
            sagaId = sagaId,
            stepName = stepName,
            status = status,
            payload = payload,
            createdAt = createdAt,
        )

    override fun compensateStep(
        saga: Saga,
        step: SagaStep,
    ) {
        when (step.stepName) {
            SagaStepNames.CREATE_USER.value -> compensateCreateUser(saga.id, step)
            SagaStepNames.CREATE_VERIFICATION_TOKEN.value -> compensateCreateVerificationToken(saga.id, step)
            SagaStepNames.UPDATE_PASSWORD.value -> compensateUpdatePassword(saga.id, step)
            SagaStepNames.UPDATE_EMAIL.value -> compensateUpdateEmail(saga.id, step)
            else -> logger.warn("No compensation defined for step ${step.stepName}")
        }
    }

    fun startSaga(
        sagaType: SagaTypes,
        payload: Any,
    ): Saga = startSaga(sagaType.value, payload)

    fun recordSagaStep(
        sagaId: String,
        stepName: SagaStepNames,
        status: SagaStepStatus,
        payload: Any? = null,
    ): SagaStep = recordSagaStep(sagaId, stepName.value, status, payload)

    /**
     * Compensate for creating a user
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
