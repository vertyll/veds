package com.vertyll.projecta.sharedinfrastructure.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for Kafka topics.
 * Each microservice defines its own topics in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "projecta.shared.kafka.topics")
class KafkaTopicsConfig {
    var all: Map<String, String> = emptyMap()
    var publish: Map<String, String> = emptyMap()
    var subscribe: Map<String, String> = emptyMap()

    companion object {
        private const val TOPIC_MAIL_REQUESTED = "mail-requested"
        private const val TOPIC_MAIL_SENT = "mail-sent"
        private const val TOPIC_MAIL_FAILED = "mail-failed"

        private const val TOPIC_SAGA_COMPENSATION = "saga-compensation"
    }

    private fun getTopic(topicKey: String): String = publish[topicKey] ?: subscribe[topicKey] ?: all[topicKey] ?: topicKey

    fun getMailRequestedTopic(): String = getTopic(TOPIC_MAIL_REQUESTED)

    fun getMailSentTopic(): String = getTopic(TOPIC_MAIL_SENT)

    fun getMailFailedTopic(): String = getTopic(TOPIC_MAIL_FAILED)

    fun getSagaCompensationTopic(): String = getTopic(TOPIC_SAGA_COMPENSATION)
}
