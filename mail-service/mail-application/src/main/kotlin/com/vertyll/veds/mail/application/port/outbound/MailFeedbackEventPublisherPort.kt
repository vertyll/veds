package com.vertyll.veds.mail.application.port.outbound

interface MailFeedbackEventPublisherPort {
    fun publishMailSent(
        originSagaId: String,
        to: String,
        subject: String,
        originalEventId: String,
    )

    fun publishMailFailed(
        originSagaId: String,
        to: String,
        subject: String,
        originalEventId: String,
        error: String,
    )
}
