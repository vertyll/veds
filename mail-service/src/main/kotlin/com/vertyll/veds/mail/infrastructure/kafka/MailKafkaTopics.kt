package com.vertyll.veds.mail.infrastructure.kafka

internal object MailKafkaTopics {
    const val MAIL_REQUESTED = "mail-requested"
    const val MAIL_SENT = "mail-sent"
    const val MAIL_FAILED = "mail-failed"
}
