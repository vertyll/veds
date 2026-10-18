package com.vertyll.veds.mail.application.port.outbound

interface MailSenderPort {
    fun sendHtml(
        from: String,
        to: String,
        subject: String,
        htmlContent: String,
        replyTo: String? = null,
    )
}
