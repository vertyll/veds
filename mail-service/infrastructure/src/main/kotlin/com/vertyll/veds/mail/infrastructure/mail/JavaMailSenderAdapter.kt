package com.vertyll.veds.mail.infrastructure.mail

import com.vertyll.veds.mail.application.port.outbound.MailSenderPort
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * Spring `JavaMailSender` adapter for the application-level [MailSenderPort].
 *
 * Translates simple port arguments into a properly configured MIME message
 * and delegates transport to the underlying Spring mail sender.
 */
@Component
internal class JavaMailSenderAdapter(
    private val mailSender: JavaMailSender,
) : MailSenderPort {
    private companion object {
        private const val CHARSET_UTF8 = "UTF-8"
    }

    override fun sendHtml(
        from: String,
        to: String,
        subject: String,
        htmlContent: String,
        replyTo: String?,
    ) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, CHARSET_UTF8)

        helper.setFrom(from)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)
        if (replyTo != null) {
            helper.setReplyTo(replyTo)
        }

        mailSender.send(message)
    }
}
