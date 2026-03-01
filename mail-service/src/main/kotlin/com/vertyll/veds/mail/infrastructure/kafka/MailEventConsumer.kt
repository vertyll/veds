package com.vertyll.veds.mail.infrastructure.kafka

import com.vertyll.veds.mail.domain.model.enums.EmailTemplate
import com.vertyll.veds.mail.domain.service.EmailSagaService
import com.vertyll.veds.sharedinfrastructure.event.EventType
import com.vertyll.veds.sharedinfrastructure.event.mail.MailFailedEvent
import com.vertyll.veds.sharedinfrastructure.event.mail.MailRequestedEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicsConfig
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
    @Suppress("unused") private val kafkaTopicsConfig: KafkaTopicsConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val FIELD_EVENT_ID = "eventId"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_EVENT_TYPE = "eventType"
        private const val FIELD_TO = "to"
        private const val FIELD_SUBJECT = "subject"
        private const val FIELD_TEMPLATE_NAME = "templateName"
        private const val FIELD_VARIABLES = "variables"
        private const val FIELD_REPLY_TO = "replyTo"
        private const val FIELD_PRIORITY = "priority"
        private const val FIELD_SAGA_ID = "sagaId"

        private val DEFAULT_EVENT_TYPE = EventType.MAIL_REQUESTED.value
        private const val DEFAULT_PRIORITY = 0
    }

    @KafkaListener(topics = ["#{@kafkaTopicsConfig.getMailRequestedTopic()}"])
    fun consume(
        record: ConsumerRecord<String, String>,
        @Payload payload: String,
    ) {
        try {
            logger.info("Received mail request message: {}", record.key())
            logger.debug("Message payload: {}", record.value())

            val event =
                try {
                    objectMapper.readValue<MailRequestedEvent>(payload)
                } catch (e: JacksonException) {
                    logger.warn("Could not directly deserialize payload, attempting manual parsing: {}", e.message)
                    val cleanPayload = cleanJsonPayload(payload)

                    val jsonNode = objectMapper.readTree(cleanPayload)
                    createMailRequestedEventFromJson(jsonNode)
                }

            handleEvent(event)
        } catch (e: Exception) {
            logger.error("Error processing message from topic {} — will be retried / sent to DLT", record.topic(), e)
            logger.error("Failed payload: {}", payload)
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
        val variablesNode = jsonNode[FIELD_VARIABLES]
        val variables =
            if (variablesNode != null) {
                try {
                    variablesNode.properties().associate { (key, value) ->
                        key to (value.asString() ?: "")
                    }
                } catch (e: Exception) {
                    logger.warn("Could not convert variables to Map<String, String>, using empty map: {}", e.message)
                    emptyMap()
                }
            } else {
                emptyMap()
            }

        val templateName = jsonNode[FIELD_TEMPLATE_NAME]?.asString() ?: ""

        return MailRequestedEvent(
            eventId = jsonNode[FIELD_EVENT_ID]?.asString() ?: "",
            timestamp =
                try {
                    objectMapper.convertValue(jsonNode[FIELD_TIMESTAMP], Instant::class.java)
                } catch (e: Exception) {
                    logger.warn("Could not convert timestamp, using current time: {}", e.message)
                    Instant.now()
                },
            eventType = jsonNode[FIELD_EVENT_TYPE]?.asString() ?: DEFAULT_EVENT_TYPE,
            to = jsonNode[FIELD_TO]?.asString() ?: "",
            subject = jsonNode[FIELD_SUBJECT]?.asString() ?: "",
            templateName = templateName,
            variables = variables,
            replyTo = jsonNode[FIELD_REPLY_TO]?.asString(),
            priority = jsonNode[FIELD_PRIORITY]?.asInt() ?: DEFAULT_PRIORITY,
            sagaId = jsonNode[FIELD_SAGA_ID]?.asString(),
        )
    }

    private fun handleEvent(event: MailRequestedEvent) {
        logger.info("Processing mail request: {}", event.eventId)

        val template = EmailTemplate.fromTemplateName(event.templateName)

        if (template != null) {
            val success =
                emailSagaService.sendEmailWithSaga(
                    to = event.to,
                    subject = event.subject,
                    template = template,
                    variables = event.variables,
                    replyTo = event.replyTo,
                    originSagaId = event.sagaId,
                    originalEventId = event.eventId,
                )

            if (success) {
                logger.info("Successfully processed mail request: {}", event.eventId)
            }
        } else {
            logger.error("Received request with invalid template name: {}. Email will not be sent.", event.templateName)

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
                    topic = KafkaTopicNames.MAIL_FAILED,
                    key = event.sagaId!!,
                    payload = failedEvent,
                    sagaId = event.sagaId!!,
                    eventId = failedEvent.eventId,
                )
                logger.info("Published MailFailedEvent for invalid template — saga: {}", event.sagaId)
            }
        }
    }
}
