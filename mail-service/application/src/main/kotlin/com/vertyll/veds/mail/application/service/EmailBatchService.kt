package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.domain.model.EmailTemplate
import org.springframework.stereotype.Service

@Service
class EmailBatchService(
    private val emailService: EmailService,
) {
    fun processEmailBatch(
        recipients: List<String>,
        subject: String,
        template: EmailTemplate,
        commonVariables: Map<String, String>,
        specificVariables: Map<String, Map<String, String>>,
        replyTo: String? = null,
    ): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        for (recipient in recipients) {
            val variables = commonVariables.toMutableMap()
            specificVariables[recipient]?.let { variables.putAll(it) }
            val success = emailService.sendEmail(recipient, subject, template, variables, replyTo)
            results[recipient] = success
        }
        return results
    }
}
