package com.vertyll.veds.mail.domain.service

import com.vertyll.veds.mail.domain.model.entity.EmailLog
import com.vertyll.veds.mail.domain.model.enums.EmailStatus
import com.vertyll.veds.mail.domain.model.enums.EmailTemplate
import com.vertyll.veds.mail.domain.repository.EmailLogRepository
import com.vertyll.veds.mail.infrastructure.config.MailProperties
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.Instant

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    private val emailLogRepository: EmailLogRepository,
    private val mailProperties: MailProperties,
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    private companion object {
        private const val CHARSET_UTF8 = "UTF-8"
        private const val MAX_VARIABLE_VALUE_LENGTH = 50

        private const val LOG_SENDING_EMAIL = "Sending email to {} with subject: {}"
        private const val LOG_SEND_FAILURE = "Failed to send email to {} with subject: {}"
    }

    /**
     * Sends an email using a template specified by the EmailTemplate enum and variables
     *
     * @param to email recipient
     * @param subject email subject
     * @param template the EmailTemplate to use
     * @param variables variables to use in the template
     * @param replyTo optional reply-to address
     * @return true if the email was sent successfully, false otherwise
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

            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, CHARSET_UTF8)

            helper.setFrom(mailProperties.from)
            helper.setTo(to)
            helper.setSubject(subject)

            if (replyTo != null) {
                helper.setReplyTo(replyTo)
            }

            val context = Context()
            variables.forEach { (key, value) ->
                context.setVariable(key, value)
            }

            val htmlContent = templateEngine.process(template.templateName, context)
            helper.setText(htmlContent, true)

            mailSender.send(message)

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

    /**
     * Formats variables for storage, truncating long values for readability
     *
     * @param variables map of variables to format
     * @return formatted string representation of variables, or null if empty
     */
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

    /**
     * Saves an email log entry to the database
     *
     * @param recipient email recipient
     * @param subject email subject
     * @param templateName name of the email template used
     * @param variables variables used in the email, formatted for storage
     * @param replyTo optional reply-to address
     * @param success whether the email was sent successfully
     * @param errorMessage optional error message if sending failed
     */
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
}
