package com.vertyll.veds.iam.infrastructure.kafka

import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicsConfig
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
