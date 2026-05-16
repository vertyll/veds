package com.vertyll.veds.mail.application.port.inbound

import com.vertyll.veds.mail.application.dto.EmailLogResponse
import com.vertyll.veds.mail.domain.model.EmailTemplate
import org.springframework.data.domain.Page

interface EmailUseCase {
    fun sendEmail(
        to: String,
        subject: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String? = null,
    ): Boolean

    fun getEmailLogs(): Page<EmailLogResponse>
}
