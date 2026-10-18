package com.vertyll.veds.mail.infrastructure.web.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendEmailRequest(
    @field:NotBlank(message = "validation.mail.recipient_email_required")
    @field:Email(message = "validation.iam.email_invalid")
    val to: String,
    @field:NotBlank(message = "validation.mail.template_name_required")
    val templateName: String,
    val variables: Map<String, String> = emptyMap(),
    val replyTo: String? = null,
)
