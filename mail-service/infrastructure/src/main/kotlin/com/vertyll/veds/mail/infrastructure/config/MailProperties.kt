package com.vertyll.veds.mail.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "application.mail")
data class MailProperties(
    val from: String,
)
