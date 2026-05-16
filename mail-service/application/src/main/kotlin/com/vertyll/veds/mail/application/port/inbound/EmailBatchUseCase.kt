package com.vertyll.veds.mail.application.port.inbound

import com.vertyll.veds.mail.domain.model.EmailTemplate

interface EmailBatchUseCase {
    fun processEmailBatch(
        recipients: List<String>,
        subject: String,
        template: EmailTemplate,
        commonVariables: Map<String, String>,
        specificVariables: Map<String, Map<String, String>>,
        replyTo: String? = null,
    ): Map<String, Boolean>
}
