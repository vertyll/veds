package com.vertyll.veds.mail.infrastructure

import jakarta.mail.Message
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.concurrent.CopyOnWriteArrayList

/** Keeps what would have reached the mail server, and refuses the recipients named in [refused]. */
class RecordingMailSender : JavaMailSenderImpl() {
    val delivered = CopyOnWriteArrayList<MimeMessage>()
    val refused = CopyOnWriteArrayList<String>()

    override fun doSend(
        mimeMessages: Array<out MimeMessage>,
        originalMessages: Array<out Any>?,
    ) {
        mimeMessages.forEach { message ->
            if (recipientOf(message) in refused) {
                throw MailSendException("550 mailbox unavailable")
            }
            delivered += message
        }
    }

    fun deliveredTo(recipient: String): List<MimeMessage> = delivered.filter { recipientOf(it) == recipient }

    private fun recipientOf(message: MimeMessage): String = message.getRecipients(Message.RecipientType.TO).single().toString()
}
