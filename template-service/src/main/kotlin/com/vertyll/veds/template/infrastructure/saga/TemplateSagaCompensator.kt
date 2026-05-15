package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensationContext
import com.vertyll.veds.sharedinfrastructure.saga.service.SagaCompensator
import com.vertyll.veds.template.application.saga.model.SagaCompensationActions
import com.vertyll.veds.template.application.saga.model.SagaStepNames
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaJpaEntity
import com.vertyll.veds.template.infrastructure.persistence.entity.SagaStepJpaEntity
import org.slf4j.LoggerFactory

internal class TemplateSagaCompensator : SagaCompensator<SagaJpaEntity, SagaStepJpaEntity> {
    private val logger = LoggerFactory.getLogger(TemplateSagaCompensator::class.java)

    override fun compensateStep(
        saga: SagaJpaEntity,
        step: SagaStepJpaEntity,
        context: SagaCompensationContext,
    ) {
        try {
            when (step.stepName) {
                SagaStepNames.PERSIST_TEMPLATE.value -> {
                    val p = context.readStepPayload(step.payload)
                    context.publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.DELETE_TEMPLATE.value,
                        mapOf("templateId" to p["templateId"]),
                    )
                }
                SagaStepNames.PUBLISH_TEMPLATE_EVENT.value -> {
                    val p = context.readStepPayload(step.payload)
                    context.publishCompensationEvent(
                        saga.id,
                        step.id ?: 0L,
                        SagaCompensationActions.LOG_TEMPLATE_COMPENSATION.value,
                        mapOf("templateId" to p["templateId"]),
                    )
                }
                else -> logger.warn("No compensation defined for step '${step.stepName}'")
            }
        } catch (e: Exception) {
            logger.error("Failed compensation for step ${step.stepName}", e)
        }
    }
}
