package com.vertyll.veds.template.application.saga.model

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue

/**
 * Saga step names for template-service.
 *
 * Replace these placeholder values with your actual business saga step names
 * when cloning this service for a new microservice.
 */
enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    PROCESS_TEMPLATE("ProcessTemplate"),
    PERSIST_TEMPLATE("PersistTemplate"),
    PUBLISH_TEMPLATE_EVENT("PublishTemplateEvent"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? = SagaStepNames.entries.find { it.value == value }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
