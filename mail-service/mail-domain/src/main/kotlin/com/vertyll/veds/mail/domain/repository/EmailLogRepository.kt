package com.vertyll.veds.mail.domain.repository

import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.model.PageRequest
import com.vertyll.veds.mail.domain.model.PageResult
import java.time.Instant

interface EmailLogRepository {
    fun save(emailLog: EmailLog): EmailLog

    fun findById(id: Long): EmailLog?

    fun findAll(pageRequest: PageRequest): PageResult<EmailLog>

    fun findByRecipient(recipient: String): List<EmailLog>

    fun findByTemplateName(templateName: String): List<EmailLog>

    fun findBySentAtBetween(
        start: Instant,
        end: Instant,
    ): List<EmailLog>

    fun countByStatusAndSentAtBetween(
        status: EmailStatus,
        start: Instant,
        end: Instant,
    ): Long

    fun findRecentFailedEmails(limit: Int): List<EmailLog>
}
