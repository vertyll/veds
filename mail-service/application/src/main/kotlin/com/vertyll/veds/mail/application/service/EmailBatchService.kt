package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailBatchUseCase
import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.domain.model.EmailTemplate
import org.springframework.stereotype.Service

@Service
internal class EmailBatchService(
    private val emailService: EmailUseCase,
) : EmailBatchUseCase {
    override fun processEmailBatch(
        recipients: List<String>,
        subject: String,
        template: EmailTemplate,
        commonVariables: Map<String, String>,
        specificVariables: Map<String, Map<String, String>>,
        replyTo: String?,
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
