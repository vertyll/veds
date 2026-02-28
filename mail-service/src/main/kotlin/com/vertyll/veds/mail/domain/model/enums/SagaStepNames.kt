package com.vertyll.veds.mail.domain.model.enums

enum class SagaStepNames(
    val value: String,
) {
    PROCESS_TEMPLATE("ProcessTemplate"),
    SEND_EMAIL("SendEmail"),
    RECORD_EMAIL_LOG("RecordEmailLog"),
    TEMPLATE_UPDATE("TemplateUpdate"),
    TEMPLATE_DELETE("TemplateDelete"),
    ;

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"

        fun fromString(value: String): SagaStepNames? = SagaStepNames.entries.find { it.value == value }

        fun compensationName(step: SagaStepNames): String = "$COMPENSATION_PREFIX${step.value}"

        fun compensationNameFromString(stepName: String): String = "$COMPENSATION_PREFIX$stepName"
    }
}
