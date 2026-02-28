package com.vertyll.veds.mail.domain.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendEmailRequest(
    @field:NotBlank(message = "Recipient email is required")
    @field:Email(message = "Invalid email format")
    val to: String,
    @field:NotBlank(message = "Subject is required")
    val subject: String,
    @field:NotBlank(message = "Template name is required")
    val templateName: String,
    val variables: Map<String, String> = emptyMap(),
    val replyTo: String? = null,
)
