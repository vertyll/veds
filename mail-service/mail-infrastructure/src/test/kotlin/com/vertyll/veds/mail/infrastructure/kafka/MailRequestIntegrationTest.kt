package com.vertyll.veds.mail.infrastructure.kafka

import com.vertyll.veds.mail.infrastructure.IntegrationTestBase
import com.vertyll.veds.mail.infrastructure.RecordingMailSender
import com.vertyll.veds.mail.mail.MailRequestedCommand
import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.messaging.event.Events
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertEquals

internal class MailRequestIntegrationTest(
    private val kafka: KafkaTemplate<String, ByteArray>,
    private val serializer: AvroPayloadSerializer,
    private val mailServer: RecordingMailSender,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    private fun request(
        eventId: String,
        to: String,
        sagaId: String,
        templateName: String = "PROJECT_MEMBER_JOINED",
    ) {
        val event =
            MailRequestedCommand
                .newBuilder()
                .setEventId(eventId)
                .setTimestamp(Events.now())
                .setTo(to)
                .setTemplateName(templateName)
                .setVariables(mapOf("projectName" to "Apollo"))
                .setReplyTo("owner@example.com")
                .setPriority(0)
                .setSagaId(sagaId)
                .build()
        val record =
            ProducerRecord<String, ByteArray>(
                MailKafkaTopics.MAIL_REQUESTED,
                eventId,
                serializer.serialize(MailKafkaTopics.MAIL_REQUESTED, event),
            )
        record.headers().add("eventId", eventId.toByteArray(StandardCharsets.UTF_8))
        kafka.send(record).get()
    }

    private fun feedbackFor(sagaId: String) = outbox.findAll().filter { it.sagaId == sagaId }.map { it.topic }

    @Test
    fun `a requested email goes out and the sender is told`() {
        request(Events.newId(), "invited@example.com", "saga-sent")

        await().atMost(TIMEOUT).until { feedbackFor("saga-sent").isNotEmpty() }
        assertEquals(1, mailServer.deliveredTo("invited@example.com").size)
        assertEquals(listOf(MailKafkaTopics.MAIL_SENT), feedbackFor("saga-sent"))
    }

    @Test
    fun `a refused delivery tells the sender it failed`() {
        mailServer.refused += "bounced@example.com"

        request(Events.newId(), "bounced@example.com", "saga-bounced")

        await().atMost(TIMEOUT).until { feedbackFor("saga-bounced").isNotEmpty() }
        assertEquals(listOf(MailKafkaTopics.MAIL_FAILED), feedbackFor("saga-bounced"))
    }

    @Test
    fun `an unknown template tells the sender it failed`() {
        request(Events.newId(), "nobody@example.com", "saga-unknown", templateName = "NO_SUCH_TEMPLATE")

        await().atMost(TIMEOUT).until { feedbackFor("saga-unknown").isNotEmpty() }
        assertEquals(listOf(MailKafkaTopics.MAIL_FAILED), feedbackFor("saga-unknown"))
        assertEquals(emptyList(), mailServer.deliveredTo("nobody@example.com"))
    }

    @Test
    fun `a redelivered request sends one email`() {
        val eventId = Events.newId()
        request(eventId, "once@example.com", "saga-once")
        await().atMost(TIMEOUT).until { feedbackFor("saga-once").isNotEmpty() }

        request(eventId, "once@example.com", "saga-once")
        request(Events.newId(), "after@example.com", "saga-after")

        await().atMost(TIMEOUT).until { feedbackFor("saga-after").isNotEmpty() }
        assertEquals(1, mailServer.deliveredTo("once@example.com").size)
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
