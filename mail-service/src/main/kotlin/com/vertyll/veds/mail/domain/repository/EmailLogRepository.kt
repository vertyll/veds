package com.vertyll.veds.mail.domain.repository

import com.vertyll.veds.mail.domain.model.entity.EmailLog
import com.vertyll.veds.mail.domain.model.enums.EmailStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EmailLogRepository : JpaRepository<EmailLog, Long> {
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

    @Query("SELECT e FROM EmailLog e WHERE e.status = 'FAILED' ORDER BY e.createdAt DESC")
    fun findRecentFailedEmails(limit: Int): List<EmailLog>
}
