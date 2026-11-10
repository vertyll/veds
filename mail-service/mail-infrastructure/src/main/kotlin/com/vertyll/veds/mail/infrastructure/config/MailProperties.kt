package com.vertyll.veds.mail.infrastructure.config

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "application.mail")
@Validated
data class MailProperties(
    @field:NotBlank
    @field:Email
    val from: String,
)
