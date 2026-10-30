package com.vertyll.veds.template.infrastructure.kafka

import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.template.TemplateRequestedEvent
import com.vertyll.veds.template.infrastructure.IntegrationTestBase
import com.vertyll.veds.template.infrastructure.persistence.repository.TemplateJpaRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals

internal class TemplateRequestIntegrationTest(
    private val kafka: KafkaTemplate<String, ByteArray>,
    private val serializer: AvroPayloadSerializer,
    private val templates: TemplateJpaRepository,
) : IntegrationTestBase() {
    private fun send(
        eventId: String,
        name: String,
    ) {
        val event =
            TemplateRequestedEvent
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setName(name)
                .setPayload("body")
                .setContent(null)
                .setSagaId(null)
                .build()
        val record =
            ProducerRecord<String, ByteArray>(
                TemplateKafkaTopics.TEMPLATE_REQUESTED,
                eventId,
                serializer.serialize(TemplateKafkaTopics.TEMPLATE_REQUESTED, event),
            )
        record.headers().add("eventId", eventId.toByteArray(StandardCharsets.UTF_8))
        kafka.send(record).get()
    }

    private fun countNamed(name: String) = templates.findAll().count { it.name == name }

    @Test
    fun `a requested template is created from the event`() {
        send(Events.newId(), "From the topic")

        await().atMost(TIMEOUT).until { countNamed("From the topic") == 1 }
    }

    @Test
    fun `a redelivered event creates nothing more`() {
        val eventId = Events.newId()
        send(eventId, "Delivered twice")
        await().atMost(TIMEOUT).until { countNamed("Delivered twice") == 1 }

        send(eventId, "Delivered twice")
        send(Events.newId(), "Delivered after")

        await().atMost(TIMEOUT).until { countNamed("Delivered after") == 1 }
        assertEquals(1, countNamed("Delivered twice"))
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
