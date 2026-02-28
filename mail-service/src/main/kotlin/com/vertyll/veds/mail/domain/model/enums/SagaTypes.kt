package com.vertyll.veds.mail.domain.model.enums

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    EMAIL_SENDING("EmailSending"),
    EMAIL_BATCH_PROCESSING("EmailBatchProcessing"),
    TEMPLATE_MANAGEMENT("TemplateManagement"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
