package com.vertyll.veds.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Kafka topics.
 * Each microservice defines its own topics in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "veds.shared.services.topics")
class KafkaTopicsConfig {
    var all: Map<String, String> = emptyMap()

    fun getMailRequestedTopic(): String = all["mail-requested"] ?: "mail-requested"

    fun getMailSentTopic(): String = all["mail-sent"] ?: "mail-sent"

    fun getMailFailedTopic(): String = all["mail-failed"] ?: "mail-failed"

    fun getSagaCompensationTopic(): String = all["saga-compensation"] ?: "saga-compensation"

    fun getUserRegisteredTopic(): String = all["user-registered"] ?: "user-registered"

    fun getUserActivatedTopic(): String = all["user-activated"] ?: "user-activated"

    fun getUserPasswordResetTopic(): String = all["user-password-reset"] ?: "user-password-reset"
}
