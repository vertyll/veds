package com.vertyll.veds.project.application.port.inbound

interface MailFeedbackUseCase {
    fun handleMailSent(
        sagaId: String?,
        to: String,
    )

    fun handleMailFailed(
        sagaId: String?,
        to: String,
        error: String,
    )
}
