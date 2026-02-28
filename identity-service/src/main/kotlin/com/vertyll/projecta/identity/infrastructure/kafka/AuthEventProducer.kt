package com.vertyll.projecta.identity.infrastructure.kafka

import com.vertyll.projecta.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaTopicsConfig
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class AuthEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val kafkaTopicsConfig: KafkaTopicsConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val HEADER = "__TypeId__"
        private const val HEADER_VALUE = "mailRequested"
    }

    /**
     * Sends a mail-requested event to the Kafka topic.
     */
    fun sendMailRequestedEvent(event: MailRequestedEvent) {
        val eventJson = objectMapper.writeValueAsString(event)
        val message =
            MessageBuilder
                .withPayload(eventJson)
                .setHeader(KafkaHeaders.KEY, event.eventId)
                .setHeader(KafkaHeaders.TOPIC, kafkaTopicsConfig.getMailRequestedTopic())
                .setHeader(HEADER, HEADER_VALUE)
                .build()
        kafkaTemplate.send(message)
        logger.info("Sent mail request to: ${event.to}")
    }
}
