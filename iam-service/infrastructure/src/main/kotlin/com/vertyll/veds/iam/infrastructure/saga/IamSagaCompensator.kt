package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.saga.model.SagaCompensationActions
import com.vertyll.veds.iam.application.saga.model.SagaStepNames
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensator
import org.slf4j.LoggerFactory

internal class IamSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity> {
    private val logger = LoggerFactory.getLogger(IamSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        when (step.stepName) {
            SagaStepNames.CREATE_USER.value -> compensateCreateUser(saga.id, step, context)
            SagaStepNames.CREATE_VERIFICATION_TOKEN.value -> compensateCreateVerificationToken(saga.id, step, context)
            SagaStepNames.UPDATE_PASSWORD.value -> compensateUpdatePassword(saga.id, step, context)
            SagaStepNames.UPDATE_EMAIL.value -> compensateUpdateEmail(saga.id, step, context)
            SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT.value ->
                logger.info("Mail event already published for saga '${saga.id}' — no compensation needed")
            SagaStepNames.PUBLISH_USER_REGISTERED_EVENT.value ->
                logger.info("User event step for saga '${saga.id}' — no compensation needed")
            else -> logger.warn("No compensation defined for step '${step.stepName}'")
        }
    }

    private fun compensateCreateUser(
        sagaId: String,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        runCatching {
            val payload = context.readStepPayload(step.payload)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()

            context.publishCompensationEvent(
                sagaId = sagaId,
                stepId = step.id,
                action = SagaCompensationActions.DELETE_USER.value,
                extraPayload = mapOf("userId" to userId),
            )
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.CREATE_USER.value}': ${e.message}", e)
        }
    }

    private fun compensateCreateVerificationToken(
        sagaId: String,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        runCatching {
            val payload = context.readStepPayload(step.payload)
            val tokenId = (payload["tokenId"] as Number).toLong()

            context.publishCompensationEvent(
                sagaId = sagaId,
                stepId = step.id,
                action = SagaCompensationActions.DELETE_VERIFICATION_TOKEN.value,
                extraPayload = mapOf("tokenId" to tokenId),
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
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        runCatching {
            val payload = context.readStepPayload(step.payload)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()
            val originalPasswordHash = payload["originalPasswordHash"]?.toString()

            if (originalPasswordHash != null) {
                context.publishCompensationEvent(
                    sagaId = sagaId,
                    stepId = step.id,
                    action = SagaCompensationActions.REVERT_PASSWORD_UPDATE.value,
                    extraPayload =
                        mapOf(
                            "userId" to userId,
                            "originalPasswordHash" to originalPasswordHash,
                        ),
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
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        runCatching {
            val payload = context.readStepPayload(step.payload)
            val userId = (payload["userId"] as? Number ?: payload["authUserId"] as Number).toLong()
            val originalEmail = payload["originalEmail"]?.toString()

            if (originalEmail != null) {
                context.publishCompensationEvent(
                    sagaId = sagaId,
                    stepId = step.id,
                    action = SagaCompensationActions.REVERT_EMAIL_UPDATE.value,
                    extraPayload =
                        mapOf(
                            "userId" to userId,
                            "originalEmail" to originalEmail,
                        ),
                )
            } else {
                logger.warn("Cannot compensate '${SagaStepNames.UPDATE_EMAIL.value}' — original email missing for user $userId")
            }
        }.onFailure { e ->
            logger.error("Failed to publish compensation event for step '${SagaStepNames.UPDATE_EMAIL.value}': ${e.message}", e)
        }
    }
}
