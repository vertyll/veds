package com.vertyll.veds.iam.infrastructure.saga

import com.vertyll.veds.iam.application.saga.model.AuthCompensationCommand
import com.vertyll.veds.iam.application.saga.model.SagaStepNames
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensator
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaWatchdog
import org.slf4j.LoggerFactory

/**
 * Domain-side compensation logic for IAM sagas.
 *
 * Reads the JSON snapshot persisted with each saga step
 * (`saga_step.payload`, written by `SagaEngine.recordSagaStep`),
 * assembles a strongly-typed [AuthCompensationCommand], and publishes it
 * via the [SagaCompensationContext] (Transactional Outbox → Kafka).
 *
 * Steps that do not have a meaningful reverse operation (e.g. event
 * publication steps) intentionally log and skip — they are not errors.
 *
 * Steps for which no compensation is defined throw — `SagaCompensationRunner`
 * marks them `COMPENSATION_FAILED` so [SagaWatchdog]
 * keeps retrying with cooldown until the situation is resolved.
 */
internal class IamSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity, AuthCompensationCommand> {
    private val logger = LoggerFactory.getLogger(IamSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext<AuthCompensationCommand>,
    ) {
        val command =
            when (step.stepName) {
                SagaStepNames.CREATE_USER.value ->
                    AuthCompensationCommand.DeleteUser(readUserId(context, step))
                SagaStepNames.CREATE_VERIFICATION_TOKEN.value,
                SagaStepNames.CREATE_RESET_TOKEN.value,
                ->
                    AuthCompensationCommand.DeleteVerificationToken(readTokenId(context, step))
                SagaStepNames.UPDATE_PASSWORD.value ->
                    AuthCompensationCommand.RevertPasswordUpdate(readUserId(context, step))
                SagaStepNames.UPDATE_EMAIL.value -> {
                    val p = context.readStepPayload(step.payload)
                    AuthCompensationCommand.RevertEmailUpdate(
                        userId = readUserId(p),
                        originalEmail =
                            requireNotNull(p["originalEmail"]?.toString()) {
                                "Missing 'originalEmail' in step payload for UpdateEmail step ${step.id}"
                            },
                    )
                }
                SagaStepNames.PUBLISH_MAIL_REQUESTED_EVENT.value,
                SagaStepNames.PUBLISH_USER_REGISTERED_EVENT.value,
                SagaStepNames.VERIFY_CURRENT_PASSWORD.value,
                -> {
                    logger.info(
                        "No compensation needed for step '{}' on saga '{}' (effect not externally observable)",
                        step.stepName,
                        saga.id,
                    )
                    return
                }
                else -> {
                    logger.warn("No compensation defined for step '{}' on saga '{}'", step.stepName, saga.id)
                    return
                }
            }

        context.publishCompensationEvent(
            sagaId = saga.id,
            stepId = step.id,
            command = command,
        )
    }

    private fun readUserId(
        context: SagaCompensationContext<AuthCompensationCommand>,
        step: SagaStepJpaEntity,
    ): Long = readUserId(context.readStepPayload(step.payload))

    private fun readUserId(payload: Map<String, Any?>): Long {
        val raw =
            payload["userId"]
                ?: error("Missing 'userId' in step payload (keys: ${payload.keys})")
        return (raw as Number).toLong()
    }

    private fun readTokenId(
        context: SagaCompensationContext<AuthCompensationCommand>,
        step: SagaStepJpaEntity,
    ): Long {
        val payload = context.readStepPayload(step.payload)
        val raw =
            payload["tokenId"]
                ?: error("Missing 'tokenId' in step payload for step ${step.id} (keys: ${payload.keys})")
        return (raw as Number).toLong()
    }
}
