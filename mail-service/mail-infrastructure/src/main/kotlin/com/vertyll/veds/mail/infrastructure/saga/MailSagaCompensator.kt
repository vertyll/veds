package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.saga.model.MailCompensationCommand
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.entity.SagaStepJpaEntity
import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensator
import org.slf4j.LoggerFactory

internal class MailSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity, MailCompensationCommand> {
    private val logger = LoggerFactory.getLogger(MailSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext<MailCompensationCommand>,
    ) {
        val command =
            when (step.stepName) {
                SagaStepNames.SEND_EMAIL.value -> {
                    val p = context.readStepPayload(step.payload)
                    MailCompensationCommand.LogEmailCompensation(
                        emailId = requireNotNull(p["emailId"]?.toString()) { "Missing 'emailId' in step ${step.id}" },
                        to = requireNotNull(p["to"]?.toString()) { "Missing 'to' in step ${step.id}" },
                    )
                }

                SagaStepNames.RECORD_EMAIL_LOG.value -> {
                    val p = context.readStepPayload(step.payload)
                    MailCompensationCommand.DeleteEmailLog(
                        logId = (requireNotNull(p["logId"]) { "Missing 'logId' in step ${step.id}" } as Number).toLong(),
                    )
                }

                SagaStepNames.TEMPLATE_UPDATE.value -> {
                    val p = context.readStepPayload(step.payload)
                    MailCompensationCommand.LogTemplateCompensation(
                        templateName = requireNotNull(p["templateName"]?.toString()) { "Missing 'templateName' in step ${step.id}" },
                    )
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
}
