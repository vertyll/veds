package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.config.MailProperties
import com.vertyll.veds.mail.application.dto.EmailLogResponse
import com.vertyll.veds.mail.application.port.out.MailSenderPort
import com.vertyll.veds.mail.application.port.out.TemplateRendererPort
import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EmailService(
    private val mailSender: MailSenderPort,
    private val templateRenderer: TemplateRendererPort,
    private val emailLogRepository: EmailLogRepository,
    private val mailProperties: MailProperties,
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    private companion object {
        private const val MAX_VARIABLE_VALUE_LENGTH = 50

        private const val LOG_SENDING_EMAIL = "Sending email to {} with subject: {}"
        private const val LOG_SEND_FAILURE = "Failed to send email to {} with subject: {}"
    }

    /**
     * Sends an email using a template specified by the EmailTemplate enum and variables.
     */
    fun sendEmail(
        to: String,
        subject: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String? = null,
    ): Boolean {
        try {
            logger.info(LOG_SENDING_EMAIL, to, subject)

            val htmlContent = templateRenderer.render(template.templateName, variables)
            mailSender.sendHtml(
                from = mailProperties.from,
                to = to,
                subject = subject,
                htmlContent = htmlContent,
                replyTo = replyTo,
            )

            saveEmailLog(
                recipient = to,
                subject = subject,
                templateName = template.templateName,
                variables = formatVariablesForStorage(variables),
                replyTo = replyTo,
                success = true,
            )

            return true
        } catch (e: Exception) {
            logger.error(LOG_SEND_FAILURE, to, subject, e)

            saveEmailLog(
                recipient = to,
                subject = subject,
                templateName = template.templateName,
                variables = formatVariablesForStorage(variables),
                replyTo = replyTo,
                success = false,
                errorMessage = e.message,
            )

            return false
        }
    }

    private fun formatVariablesForStorage(variables: Map<String, String>): String? {
        if (variables.isEmpty()) {
            return null
        }

        return variables.entries.joinToString(", ") { (key, value) ->
            if (value.length > MAX_VARIABLE_VALUE_LENGTH) {
                "$key: ${value.take(MAX_VARIABLE_VALUE_LENGTH)}..."
            } else {
                "$key: $value"
            }
        }
    }

    private fun saveEmailLog(
        recipient: String,
        subject: String,
        templateName: String,
        variables: String? = null,
        replyTo: String? = null,
        success: Boolean,
        errorMessage: String? = null,
    ) {
        val emailLog =
            EmailLog(
                recipient = recipient,
                subject = subject,
                templateName = templateName,
                variables = variables,
                replyTo = replyTo,
                status = if (success) EmailStatus.SENT else EmailStatus.FAILED,
                errorMessage = errorMessage,
                sentAt = if (success) Instant.now() else null,
            )

        emailLogRepository.save(emailLog)
    }

    @Transactional(readOnly = true)
    fun getEmailLogs(): Page<EmailLogResponse> = Page.empty()
}
