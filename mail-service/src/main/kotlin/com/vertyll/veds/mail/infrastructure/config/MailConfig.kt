package com.vertyll.veds.mail.infrastructure.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
@EnableConfigurationProperties(MailProperties::class)
class MailConfig {
    @Bean
    fun javaMailSender(mailProperties: MailProperties): JavaMailSender =
        JavaMailSenderImpl().apply {
            host = mailProperties.host
            mailProperties.port?.let { port = it }

            username = mailProperties.username
            password = mailProperties.password
            defaultEncoding = "UTF-8"

            if (mailProperties.properties.isNotEmpty()) {
                javaMailProperties.putAll(mailProperties.properties)
            }
        }
}
