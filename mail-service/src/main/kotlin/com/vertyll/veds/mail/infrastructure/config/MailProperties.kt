package com.vertyll.veds.mail.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.mail")
data class MailProperties(
    val host: String,
    val port: Int?,
    val username: String,
    val password: String,
    val from: String,
    val properties: Map<String, String> = emptyMap(),
)
