package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxEntity
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.SendResult
import org.springframework.messaging.Message
import java.util.concurrent.CompletableFuture
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The poller is what turns a promised event into a published one. Everything it does happens after
 * the business transaction has already committed, so a message it drops is lost outright and one it
 * gives up on too early is an event the rest of the system never sees.
 */
internal class OutboxDispatchTest {
    private val sent = mutableListOf<Message<*>>()
    private var failWith: Exception? = null

    /** A broker that records what it was asked to publish, and can be told to refuse. */
    private inner class RecordingKafkaTemplate : KafkaTemplate<String, ByteArray>(DefaultKafkaProducerFactory(emptyMap())) {
        override fun send(message: Message<*>): CompletableFuture<SendResult<String, ByteArray>> {
            failWith?.let { return CompletableFuture.failedFuture(it) }
            sent += message
            val topic = requireNotNull(message.headers[KafkaHeaders.TOPIC]) { "a message names its topic" } as String
            return CompletableFuture.completedFuture(
                SendResult(
                    ProducerRecord(topic, "key", byteArrayOf()),
                    RecordMetadata(TopicPartition(topic, 0), 0L, 0, 0L, 0, 0),
                ),
            )
        }
    }

    private val repository = InMemoryOutboxRepository()
    private val properties = KafkaOutboxProperties(maxRetries = 3)

    private val processor =
        KafkaOutboxProcessor(
            outboxRepository = repository,
            outboxMessageFactory = TestOutboxMessageFactory(),
            kafkaTemplate = RecordingKafkaTemplate(),
            properties = properties,
            dispatchTx = OutboxDispatchTx(repository),
        )

    private fun givenClaimable(vararg messages: OutboxEntity) {
        repository.claimable = messages.toList()
    }

    @Test
    fun `an empty outbox publishes nothing`() {
        processor.pollAndDispatch()

        assertTrue(sent.isEmpty())
        assertTrue(repository.saved.isEmpty(), "an idle poll should not write to the outbox")
    }

    /** Claiming first is what stops a second instance of the service publishing the same row. */
    @Test
    fun `a claimed row is marked in flight before it is published`() {
        givenClaimable(testOutboxMessage(eventId = "e-1"))

        processor.pollAndDispatch()

        assertEquals(
            listOf(OutboxStatus.PROCESSING, OutboxStatus.COMPLETED),
            repository.transitions.map { it.second },
        )
    }

    @Test
    fun `the published message carries the row's topic, key and payload`() {
        givenClaimable(
            testOutboxMessage(eventId = "e-1", topic = "project-created", key = "project-9", payload = byteArrayOf(4, 2)),
        )

        processor.pollAndDispatch()

        val message = sent.single()
        assertEquals("project-created", message.headers[KafkaHeaders.TOPIC])
        assertEquals("project-9", message.headers[KafkaHeaders.KEY])
        assertContentEquals(byteArrayOf(4, 2), message.payload as ByteArray)
    }

    /** Consumers deduplicate on this header — without it an at-least-once delivery becomes a duplicate. */
    @Test
    fun `the event id travels with the message`() {
        givenClaimable(testOutboxMessage(eventId = "e-1"))

        processor.pollAndDispatch()

        assertEquals("e-1", sent.single().headers["eventId"])
    }

    @Test
    fun `a published row is completed and not offered again`() {
        givenClaimable(testOutboxMessage(eventId = "e-1"))

        processor.pollAndDispatch()

        assertEquals(OutboxStatus.COMPLETED, repository.latest("e-1").status)
    }

    @Test
    fun `the whole claimed batch is published`() {
        givenClaimable(
            testOutboxMessage(eventId = "e-1"),
            testOutboxMessage(eventId = "e-2"),
            testOutboxMessage(eventId = "e-3"),
        )

        processor.pollAndDispatch()

        assertEquals(listOf("e-1", "e-2", "e-3"), sent.map { it.headers["eventId"] })
    }

    /**
     * A broker that is momentarily unavailable must not cost the event. The row goes back to
     * PENDING with the attempt counted, which is what the next poll picks up.
     */
    @Test
    fun `a refused publish is queued for another attempt`() {
        givenClaimable(testOutboxMessage(eventId = "e-1"))
        failWith = IllegalStateException("broker unavailable")

        processor.pollAndDispatch()

        val row = repository.latest("e-1")
        assertEquals(OutboxStatus.PENDING, row.status)
        assertEquals(1, row.retryCount)
        assertTrue(row.errorMessage!!.contains("broker unavailable"))
    }

    /**
     * Retrying forever would keep a permanently undeliverable message in every batch, starving the
     * rows behind it. On the last allowed attempt the row leaves the queue for good.
     */
    @Test
    fun `a message that exhausts its attempts is dead-lettered`() {
        givenClaimable(testOutboxMessage(eventId = "e-1", retryCount = properties.maxRetries - 1))
        failWith = IllegalStateException("still unavailable")

        processor.pollAndDispatch()

        assertEquals(OutboxStatus.DEAD_LETTERED, repository.latest("e-1").status)
    }

    @Test
    fun `a message one attempt short of the limit is still retried`() {
        givenClaimable(testOutboxMessage(eventId = "e-1", retryCount = properties.maxRetries - 2))
        failWith = IllegalStateException("still unavailable")

        processor.pollAndDispatch()

        assertEquals(OutboxStatus.PENDING, repository.latest("e-1").status)
    }

    /** One undeliverable row must not hold up the rest of the batch behind it. */
    @Test
    fun `a failure does not abandon the rest of the batch`() {
        givenClaimable(testOutboxMessage(eventId = "e-1"), testOutboxMessage(eventId = "e-2"))
        failWith = IllegalStateException("broker unavailable")

        processor.pollAndDispatch()

        assertEquals(OutboxStatus.PENDING, repository.latest("e-1").status)
        assertEquals(OutboxStatus.PENDING, repository.latest("e-2").status)
    }

    @Test
    fun `no more than a batch is claimed at once`() {
        givenClaimable(*(1..10).map { testOutboxMessage(eventId = "e-$it") }.toTypedArray())

        KafkaOutboxProcessor(
            outboxRepository = repository,
            outboxMessageFactory = TestOutboxMessageFactory(),
            kafkaTemplate = RecordingKafkaTemplate(),
            properties = KafkaOutboxProperties(batchSize = 4),
            dispatchTx = OutboxDispatchTx(repository),
        ).pollAndDispatch()

        assertEquals(4, sent.size)
    }
}
