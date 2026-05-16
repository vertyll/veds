package com.vertyll.veds.mail.application.port.out

/**
 * Outbound port for sending an HTML email.
 *
 * Implemented in the infrastructure layer by an adapter wrapping a concrete
 * mail transport (e.g. Spring `JavaMailSender` over SMTP). The application
 * service depends only on this contract — it must not be aware of MIME,
 * SMTP, or any framework-specific mail abstractions.
 */
interface MailSenderPort {
    /**
     * Sends a UTF-8 HTML email synchronously.
     *
     * @param from sender address
     * @param to recipient address
     * @param subject subject line
     * @param htmlContent fully-rendered HTML body
     * @param replyTo optional reply-to address
     * @throws Exception if transport fails — the caller decides how to log/recover
     */
    fun sendHtml(
        from: String,
        to: String,
        subject: String,
        htmlContent: String,
        replyTo: String? = null,
    )
}
