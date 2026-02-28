package com.vertyll.veds.template.domain.model.enums

enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    EXAMPLE_STEP("exampleStep"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? = SagaStepNames.entries.find { it.value == value }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
