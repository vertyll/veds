package com.vertyll.veds.iam.domain.service

import com.vertyll.veds.iam.domain.model.entity.Saga
import com.vertyll.veds.iam.domain.model.entity.SagaStep
import com.vertyll.veds.iam.domain.model.enums.SagaCompensationActions
import com.vertyll.veds.iam.domain.model.enums.SagaStepNames
import com.vertyll.veds.iam.domain.model.enums.SagaTypes
import com.vertyll.veds.iam.domain.repository.SagaRepository
import com.vertyll.veds.iam.domain.repository.SagaStepRepository
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.service.BaseSagaManager
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * IAM-specific saga manager.
 *
 * Enum-typed overloads (SagaTypes / SagaStepNames) are inherited from
 * [BaseSagaManager] and delegate through the Spring proxy automatically
 * via ApplicationContextAware — no wrapper methods needed here.
 *
 * Usage:
 * ```kotlin
 * sagaManager.startSaga(SagaTypes.USER_REGISTRATION, payload)
 * sagaManager.recordSagaStep(sagaId, SagaStepNames.CREATE_USER, SagaStepStatus.COMPLETED)
 * ```
 */
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
            else -> logger.warn("No compensation defined for step '${step.stepName}'")
        }
    }

    private fun compensateCreateUser(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
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
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.CREATE_USER.value}': ${e.message}", e)
        }
    }

    private fun compensateCreateVerificationToken(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
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
        }.onFailure { e ->
            logger.error(
                "Failed to publish compensation event for step '${SagaStepNames.CREATE_VERIFICATION_TOKEN.value}': ${e.message}",
                e,
            )
        }
    }

    private fun compensateUpdatePassword(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
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
                logger.warn("Cannot compensate '${SagaStepNames.UPDATE_PASSWORD.value}' — original password hash missing for user $userId")
            }
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.UPDATE_PASSWORD.value}': ${e.message}", e)
        }
    }

    private fun compensateUpdateEmail(
        sagaId: String,
        step: SagaStep,
    ) {
        runCatching {
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
                logger.warn("Cannot compensate '${SagaStepNames.UPDATE_EMAIL.value}' — original email missing for user $userId")
            }
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.UPDATE_EMAIL.value}': ${e.message}", e)
        }
    }
}
