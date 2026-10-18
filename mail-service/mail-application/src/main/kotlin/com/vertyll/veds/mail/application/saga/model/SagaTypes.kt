package com.vertyll.veds.mail.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaTypes(
    override val value: String,
) : SagaTypeValue {
    EMAIL_SENDING("EmailSending"),
    EMAIL_BATCH_PROCESSING("EmailBatchProcessing"),
    TEMPLATE_MANAGEMENT("TemplateManagement"),
}
