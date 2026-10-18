package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.dto.EmailLogResponse
import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.application.port.outbound.MailSenderPort
import com.vertyll.veds.mail.application.port.outbound.TemplateRendererPort
import com.vertyll.veds.mail.application.port.outbound.UseCaseLogger
import com.vertyll.veds.mail.domain.model.EmailLog
import com.vertyll.veds.mail.domain.model.EmailStatus
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.mail.domain.model.PageResult
import com.vertyll.veds.mail.domain.model.SenderAddress
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import java.time.Instant

class EmailService(
    private val mailSender: MailSenderPort,
    private val templateRenderer: TemplateRendererPort,
    private val emailLogRepository: EmailLogRepository,
    private val senderAddress: SenderAddress,
    private val logger: UseCaseLogger,
) : EmailUseCase {
    private companion object {
        private const val MAX_VARIABLE_VALUE_LENGTH = 50

        private const val LOG_SENDING_EMAIL = "Sending email to {} with subject: {}"
        private const val LOG_SEND_FAILURE = "Failed to send email to {} with subject: {}"
    }

    override fun sendEmail(
        to: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String?,
    ): Boolean {
        val subject = template.subject

        try {
            logger.info(LOG_SENDING_EMAIL, to, subject)

            val htmlContent = templateRenderer.render(template.templateName, variables)
            mailSender.sendHtml(
                from = senderAddress.value,
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
        replyTo: String?,
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

    override fun getEmailLogs(): PageResult<EmailLogResponse> = PageResult(content = emptyList(), page = 0, size = 0, totalElements = 0)
}
