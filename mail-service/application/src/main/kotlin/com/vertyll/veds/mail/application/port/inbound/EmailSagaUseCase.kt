package com.vertyll.veds.mail.application.port.inbound

interface EmailSagaUseCase {
    fun sendEmailWithSaga(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String? = null,
        originSagaId: String? = null,
        originalEventId: String? = null,
    ): Boolean
}
