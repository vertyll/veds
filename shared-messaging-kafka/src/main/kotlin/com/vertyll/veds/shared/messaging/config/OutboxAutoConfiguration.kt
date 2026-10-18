package com.vertyll.veds.shared.messaging.config

import com.vertyll.veds.shared.messaging.kafka.KafkaOutboxProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableConfigurationProperties(KafkaOutboxProperties::class)
internal class OutboxAutoConfiguration
