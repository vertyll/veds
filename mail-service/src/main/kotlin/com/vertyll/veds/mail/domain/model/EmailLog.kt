package com.vertyll.veds.mail.domain.model

import java.time.Instant

data class EmailLog(
    val id: Long? = null,
    val recipient: String,
    val subject: String,
    val templateName: String,
    val variables: String? = null,
    val replyTo: String? = null,
    val status: EmailStatus = EmailStatus.PENDING,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
) {
    fun markAsSent(): EmailLog =
        copy(
            status = EmailStatus.SENT,
            sentAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun markAsFailed(error: String): EmailLog =
        copy(
            status = EmailStatus.FAILED,
            errorMessage = error,
            updatedAt = Instant.now(),
        )
}
