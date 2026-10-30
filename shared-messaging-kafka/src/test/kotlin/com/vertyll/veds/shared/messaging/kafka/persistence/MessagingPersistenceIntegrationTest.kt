package com.vertyll.veds.shared.messaging.kafka.persistence

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessage
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import com.vertyll.veds.shared.messaging.kafka.contract.ProcessedEvent
import com.vertyll.veds.shared.messaging.kafka.persistence.inbox.ProcessedEventEntity
import com.vertyll.veds.shared.messaging.kafka.persistence.inbox.ProcessedEventJpaAdapter
import com.vertyll.veds.shared.messaging.kafka.persistence.inbox.ProcessedEventRepository
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxEntity
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxJpaAdapter
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The outbox and inbox adapters against a real PostgreSQL, whose row locks and constraints they rely on. */
@Tag("integration")
@SpringBootTest(classes = [MessagingPersistenceIntegrationTest.Application::class])
@ActiveProfiles("test")
internal class MessagingPersistenceIntegrationTest(
    private val outbox: OutboxJpaAdapter,
    private val inbox: ProcessedEventJpaAdapter,
    private val outboxRows: OutboxRepository,
    private val inboxRows: ProcessedEventRepository,
    private val transactions: TransactionTemplate,
) {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = [OutboxEntity::class, ProcessedEventEntity::class])
    @EnableJpaRepositories(basePackageClasses = [OutboxRepository::class, ProcessedEventRepository::class])
    @Import(OutboxJpaAdapter::class, ProcessedEventJpaAdapter::class)
    class Application

    private fun message(
        sagaId: String? = null,
        eventId: String? = null,
    ) = outbox.create(topic = "orders", key = "key", payload = byteArrayOf(1, 2), sagaId = sagaId, eventId = eventId)

    /** Implementations of the ports' contracts that are no entities, as a non-JPA caller would hand in. */
    private class DetachedMessage(
        message: OutboxMessage,
    ) : OutboxMessage by message

    private class DetachedEvent(
        event: ProcessedEvent,
    ) : ProcessedEvent by event

    @Test
    fun `a message is stored and found by its saga, its event and its status`() {
        val saved = outbox.save(message(sagaId = "saga-found", eventId = "event-found"))

        assertEquals(listOf(saved.id), outbox.findBySagaId("saga-found").map { it.id })
        assertEquals(saved.id, assertNotNull(outbox.findByEventId("event-found")).id)
        assertTrue(outbox.findByStatus(OutboxStatus.PENDING).any { it.id == saved.id })
        assertNull(outbox.findByEventId("no-such-event"))
    }

    @Test
    fun `a message without an event id is given one`() {
        assertTrue(message().eventId.isNotBlank())
    }

    @Test
    fun `a message that is not an entity is copied into one`() {
        val retried = message(sagaId = "saga-detached").markProcessing().markRetryScheduled("once failed")

        val saved = outbox.save(DetachedMessage(retried))

        val read = assertNotNull(outbox.findByEventId(saved.eventId))
        assertEquals("saga-detached", read.sagaId)
        assertEquals("once failed", read.errorMessage)
        assertEquals(1, read.retryCount)
    }

    @Test
    fun `a dispatch batch holds the pending messages, no more than asked`() {
        outboxRows.deleteAllInBatch()
        repeat(BATCH + 1) { outbox.save(message()) }

        val batch =
            transactions.execute {
                outbox.lockBatchForDispatch(
                    maxRetries = 3,
                    retriableBefore = Instant.now().plus(Duration.ofMinutes(1)),
                    stuckBefore = Instant.now().minus(Duration.ofMinutes(1)),
                    batchSize = BATCH,
                )
            }

        assertEquals(BATCH, batch.size)
    }

    @Test
    fun `a consumed event is remembered per consumer group`() {
        inbox.insert(inbox.create("event-seen", "group-a"))

        assertTrue(inbox.exists("event-seen", "group-a"))
        assertFalse(inbox.exists("event-seen", "group-b"))
    }

    @Test
    fun `an event that is not an entity is copied into one`() {
        inbox.insert(DetachedEvent(inbox.create("event-copied", "group-a")))

        assertTrue(inbox.exists("event-copied", "group-a"))
    }

    @Test
    fun `the same event cannot be claimed twice by one group`() {
        inbox.insert(inbox.create("event-twice", "group-a"))

        assertFailsWith<DataIntegrityViolationException> { inbox.insert(inbox.create("event-twice", "group-a")) }
        assertEquals(1, inboxRows.findAll().count { it.eventId == "event-twice" })
    }

    companion object {
        private const val BATCH = 2

        private val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
                .withStartupTimeout(Duration.ofMinutes(2))
                .withStartupAttempts(3)
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
            registry.add("spring.kafka.consumer.group-id") { "messaging-test" }
            registry.add("spring.kafka.consumer.auto-offset-reset") { "earliest" }
        }
    }
}
