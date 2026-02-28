package com.vertyll.veds.mail.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class SendBatchEmailRequest(
    @field:NotEmpty(message = "At least one recipient is required")
    val recipients: List<String>,
    @field:NotBlank(message = "Subject is required")
    val subject: String,
    @field:NotBlank(message = "Template name is required")
    val templateName: String,
    val commonVariables: Map<String, String> = emptyMap(),
    val specificVariables: Map<String, Map<String, String>> = emptyMap(),
    val replyTo: String? = null,
)
