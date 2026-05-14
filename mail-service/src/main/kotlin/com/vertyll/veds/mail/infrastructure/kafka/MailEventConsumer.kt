package com.vertyll.veds.mail.infrastructure.kafka

import com.vertyll.veds.mail.application.service.EmailSagaService
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.sharedinfrastructure.event.EventType
import com.vertyll.veds.sharedinfrastructure.event.mail.MailFailedEvent
import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant

@Component
class MailEventConsumer(
    private val objectMapper: ObjectMapper,
    private val emailSagaService: EmailSagaService,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [KafkaTopicNames.Topics.MAIL_REQUESTED])
    fun consume(
        record: ConsumerRecord<String, String>,
        @Payload payload: String,
    ) {
        try {
            logger.info("Received mail request message: {}", record.key())
            val event =
                try {
                    objectMapper.readValue<MailRequestedEvent>(payload)
                } catch (_: JacksonException) {
                    logger.warn("Could not directly deserialize payload, attempting manual parsing")
                    val cleanPayload = cleanJsonPayload(payload)
                    createMailRequestedEventFromJson(objectMapper.readTree(cleanPayload))
                }

            val template = EmailTemplate.fromTemplateName(event.templateName)
            if (template != null) {
                emailSagaService.sendEmailWithSaga(
                    to = event.to,
                    subject = event.subject,
                    template = template,
                    variables = event.variables,
                    replyTo = event.replyTo,
                    originSagaId = event.sagaId,
                    originalEventId = event.eventId,
                )
            } else {
                logger.error("Invalid template name: {}. Email will not be sent.", event.templateName)
                if (event.sagaId != null) {
                    val failedEvent =
                        MailFailedEvent(
                            to = event.to,
                            subject = event.subject,
                            originalEventId = event.eventId,
                            error = "Invalid template name: ${event.templateName}",
                            sagaId = event.sagaId,
                        )
                    kafkaOutboxProcessor.saveOutboxMessage(
                        KafkaTopicNames.MAIL_FAILED,
                        event.sagaId!!,
                        failedEvent,
                        event.sagaId!!,
                        failedEvent.eventId,
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing message from topic {}", record.topic(), e)
            throw e
        }
    }

    private fun cleanJsonPayload(payload: String): String =
        if (payload.startsWith("\"") && payload.endsWith("\"")) {
            payload.substring(1, payload.length - 1).replace("\\\"", "\"")
        } else {
            payload
        }

    private fun createMailRequestedEventFromJson(jsonNode: JsonNode): MailRequestedEvent {
        val variables =
            jsonNode["variables"]?.takeIf { !it.isNull }?.properties()?.associate { (k, v) -> k to (v.asString() ?: "") } ?: emptyMap()
        return MailRequestedEvent(
            eventId = jsonNode["eventId"]?.asString() ?: "",
            timestamp = runCatching { objectMapper.convertValue(jsonNode["timestamp"], Instant::class.java) }.getOrDefault(Instant.now()),
            eventType = jsonNode["eventType"]?.asString() ?: EventType.MAIL_REQUESTED.value,
            to = jsonNode["to"]?.asString() ?: "",
            subject = jsonNode["subject"]?.asString() ?: "",
            templateName = jsonNode["templateName"]?.asString() ?: "",
            variables = variables,
            replyTo = jsonNode["replyTo"]?.asString(),
            priority = jsonNode["priority"]?.asInt() ?: 0,
            sagaId = jsonNode["sagaId"]?.asString(),
        )
    }
}
