package com.vertyll.veds.mail.infrastructure.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class SendBatchEmailRequest(
    @field:NotEmpty(message = "validation.mail.recipients_required")
    val recipients: List<String>,
    @field:NotBlank(message = "validation.mail.template_name_required")
    val templateName: String,
    val commonVariables: Map<String, String> = emptyMap(),
    val specificVariables: Map<String, Map<String, String>> = emptyMap(),
    val replyTo: String? = null,
)
