package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.shared.saga.engine.SagaCompensationContext
import com.vertyll.veds.shared.saga.engine.SagaCompensator
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import org.slf4j.LoggerFactory

internal class TemplateSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity, TemplateCompensationCommand> {
    private val logger = LoggerFactory.getLogger(TemplateSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext<TemplateCompensationCommand>,
    ) {
        val command =
            when (step.stepName) {
                SagaStepNames.PERSIST_TEMPLATE.value -> {
                    TemplateCompensationCommand.DeleteTemplate(readTemplateId(context, step))
                }

                SagaStepNames.PUBLISH_TEMPLATE_EVENT.value -> {
                    TemplateCompensationCommand.LogTemplateCompensation(readTemplateId(context, step))
                }

                SagaStepNames.PROCESS_TEMPLATE.value -> {
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

    private fun readTemplateId(
        context: SagaCompensationContext<TemplateCompensationCommand>,
        step: SagaStepJpaEntity,
    ): String {
        val payload = context.readStepPayload(step.payload)
        val raw =
            payload["templateId"]
                ?: error("Missing 'templateId' in step payload for step ${step.id} (keys: ${payload.keys})")
        return raw.toString()
    }
}
