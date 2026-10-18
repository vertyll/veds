package com.vertyll.veds.mail.infrastructure.persistence.repository

import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.infrastructure.persistence.entity.EmailLogJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
internal interface EmailLogJpaRepository : JpaRepository<EmailLogJpaEntity, Long> {
    fun findByRecipient(recipient: String): List<EmailLogJpaEntity>

    fun findByTemplateName(templateName: String): List<EmailLogJpaEntity>

    fun findBySentAtBetween(
        start: Instant,
        end: Instant,
    ): List<EmailLogJpaEntity>

    fun countByStatusAndSentAtBetween(
        status: EmailStatus,
        start: Instant,
        end: Instant,
    ): Long

    @Query("SELECT e FROM EmailLogJpaEntity e WHERE e.status = 'FAILED' ORDER BY e.createdAt DESC")
    fun findRecentFailedEmails(): List<EmailLogJpaEntity>
}
