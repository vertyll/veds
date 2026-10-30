package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEvent
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventFactory
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEventRepositoryPort
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The claim decides whether a delivery is handled or skipped, so getting it wrong either drops
 * messages or processes them twice. The transactional half of the contract — that a failed
 * handler leaves nothing claimed — needs a database and lives in project-service's integration
 * tests; what is checked here is the decision itself.
 */
class ProcessedEventGuardTest {
    private data class TestProcessedEvent(
        override val id: Long? = null,
        override val eventId: String,
        override val consumerGroup: String,
        override val processedAt: Instant = Instant.now(),
    ) : ProcessedEvent

    private val ledger = mutableSetOf<Pair<String, String>>()
    private var failNextInsert = false

    private val repository =
        object : ProcessedEventRepositoryPort {
            override fun insert(processedEvent: ProcessedEvent): ProcessedEvent {
                if (failNextInsert) throw DataIntegrityViolationException("uk_processed_event_event_id_consumer")
                ledger += processedEvent.eventId to processedEvent.consumerGroup
                return processedEvent
            }

            override fun exists(
                eventId: String,
                consumerGroup: String,
            ): Boolean = Pair(eventId, consumerGroup) in ledger
        }

    private val factory =
        object : ProcessedEventFactory {
            override fun create(
                eventId: String,
                consumerGroup: String,
            ): ProcessedEvent = TestProcessedEvent(eventId = eventId, consumerGroup = consumerGroup)
        }

    private val guard = ProcessedEventGuard(repository, factory)

    @Test
    fun `claims an event the group has not seen`() {
        assertTrue(guard.claim("e-1", "project-service:mail"))
        assertTrue(repository.exists("e-1", "project-service:mail"))
    }

    @Test
    fun `skips an event the group already handled`() {
        guard.claim("e-1", "project-service:mail")

        assertFalse(guard.claim("e-1", "project-service:mail"))
    }

    /**
     * The ledger is keyed by (event, group). Two contexts reacting to the same event is the normal
     * case in a choreographed flow, not a duplicate.
     */
    @Test
    fun `lets a different consumer group handle the same event`() {
        guard.claim("e-1", "project-service:mail")

        assertTrue(guard.claim("e-1", "task-service:mail"))
    }

    /**
     * Two consumers racing on one event both pass the existence check and one loses on the unique
     * constraint. That exception is deliberately not caught: inside the handler's transaction it
     * can only be resolved by rolling back, and the redelivery then sees the committed row.
     */
    @Test
    fun `lets a lost race surface instead of reporting a duplicate`() {
        failNextInsert = true

        assertFailsWith<DataIntegrityViolationException> { guard.claim("e-1", "project-service:mail") }
    }

    @Test
    fun `records the pair it was asked to claim`() {
        guard.claim("e-9", "notification-service:task")

        assertEquals(setOf("e-9" to "notification-service:task"), ledger)
    }
}
