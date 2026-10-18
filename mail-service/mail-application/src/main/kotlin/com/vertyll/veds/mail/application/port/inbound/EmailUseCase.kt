package com.vertyll.veds.mail.application.port.inbound

import com.vertyll.veds.mail.application.dto.EmailLogResponse
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.mail.domain.model.PageResult

interface EmailUseCase {
    fun sendEmail(
        to: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String? = null,
    ): Boolean

    fun getEmailLogs(): PageResult<EmailLogResponse>
}
