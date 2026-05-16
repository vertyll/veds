package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.saga.model.SagaCompensationActions
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensator
import org.slf4j.LoggerFactory

class MailSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity> {
    private val logger = LoggerFactory.getLogger(MailSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        try {
            when (step.stepName) {
                SagaStepNames.SEND_EMAIL.value -> {
                    val p = context.readStepPayload(step.payload)
                    context.publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.LOG_EMAIL_COMPENSATION.value,
                        mapOf(
                            "emailId" to p["emailId"],
                            "to" to p["to"],
                            "message" to "Email cannot be unsent",
                        ),
                    )
                }
                SagaStepNames.RECORD_EMAIL_LOG.value -> {
                    val p = context.readStepPayload(step.payload)
                    context.publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.DELETE_EMAIL_LOG.value,
                        mapOf("logId" to p["logId"]),
                    )
                }
                SagaStepNames.TEMPLATE_UPDATE.value -> {
                    val p = context.readStepPayload(step.payload)
                    context.publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                        mapOf("templateName" to p["templateName"]),
                    )
                }
                else -> logger.warn("No compensation defined for step '${step.stepName}'")
            }
        } catch (e: Exception) {
            logger.error("Failed compensation for step ${step.stepName}", e)
        }
    }
}
