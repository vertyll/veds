package com.vertyll.veds.mail.domain.model.enums

enum class SagaTypes(
    val value: String,
) {
    EMAIL_SENDING("EmailSending"),
    EMAIL_BATCH_PROCESSING("EmailBatchProcessing"),
    TEMPLATE_MANAGEMENT("TemplateManagement"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
