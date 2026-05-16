package com.vertyll.veds.mail.infrastructure.persistence.adapter

import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import com.vertyll.veds.mail.infrastructure.persistence.entity.EmailLogJpaEntity
import com.vertyll.veds.mail.infrastructure.persistence.repository.EmailLogJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
internal class EmailLogPersistenceAdapter(
    private val repository: EmailLogJpaRepository,
) : EmailLogRepository {
    override fun save(emailLog: EmailLog): EmailLog = repository.save(emailLog.toJpaEntity()).toDomain()

    override fun findById(id: Long): EmailLog? = repository.findByIdOrNull(id)?.toDomain()

    override fun findByRecipient(recipient: String): List<EmailLog> = repository.findByRecipient(recipient).map { it.toDomain() }

    override fun findByTemplateName(templateName: String): List<EmailLog> =
        repository.findByTemplateName(templateName).map { it.toDomain() }

    override fun findBySentAtBetween(
        start: Instant,
        end: Instant,
    ): List<EmailLog> =
        repository.findBySentAtBetween(start, end).map {
            it.toDomain()
        }

    override fun countByStatusAndSentAtBetween(
        status: EmailStatus,
        start: Instant,
        end: Instant,
    ): Long = repository.countByStatusAndSentAtBetween(status, start, end)

    override fun findRecentFailedEmails(limit: Int): List<EmailLog> = repository.findRecentFailedEmails().take(limit).map { it.toDomain() }
}

private fun EmailLog.toJpaEntity() =
    EmailLogJpaEntity(
        id = this.id,
        recipient = this.recipient,
        subject = this.subject,
        templateName = this.templateName,
        variables = this.variables,
        replyTo = this.replyTo,
        status = this.status,
        errorMessage = this.errorMessage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        sentAt = this.sentAt,
    )

private fun EmailLogJpaEntity.toDomain() =
    EmailLog(
        id = this.id,
        recipient = this.recipient,
        subject = this.subject,
        templateName = this.templateName,
        variables = this.variables,
        replyTo = this.replyTo,
        status = this.status,
        errorMessage = this.errorMessage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        sentAt = this.sentAt,
    )
