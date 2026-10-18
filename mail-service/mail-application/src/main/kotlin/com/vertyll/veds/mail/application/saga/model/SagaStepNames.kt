package com.vertyll.veds.mail.application.saga.model

import com.vertyll.veds.shared.saga.SagaTypeValue

enum class SagaStepNames(
    override val value: String,
) : SagaTypeValue {
    PROCESS_TEMPLATE("ProcessTemplate"),
    SEND_EMAIL("SendEmail"),
    RECORD_EMAIL_LOG("RecordEmailLog"),
    TEMPLATE_UPDATE("TemplateUpdate"),
    TEMPLATE_DELETE("TemplateDelete"),
}
