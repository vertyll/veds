package com.vertyll.veds.sharedinfrastructure.kafka.contract

import java.time.Instant

/**
 * Persistence-agnostic repository port for the Kafka outbox.
 *
 * Concrete adapters (JPA, MongoDB, Cassandra, …) implement this interface;
 * the outbox processor is decoupled from the underlying storage technology.
 */
interface OutboxRepositoryPort {
    fun save(message: OutboxMessage): OutboxMessage

    fun findByStatus(status: OutboxStatus): List<OutboxMessage>

    fun findBySagaId(sagaId: String): List<OutboxMessage>

    fun findByEventId(eventId: String): OutboxMessage?

    /**
     * Atomically selects up to [batchSize] messages eligible for dispatch
     * and locks them with a write lock so that concurrent poller instances
     * skip them (`SELECT … FOR UPDATE SKIP LOCKED` on PostgreSQL).
     *
     * A message is eligible when:
     *  - it is [OutboxStatus.PENDING] (initial state or rescheduled retry)
     *    whose cooldown has elapsed (`lastRetryAt` is `null` or earlier than
     *    [retriableBefore]), AND
     *  - it has been attempted fewer than [maxRetries] times, OR
     *  - it is [OutboxStatus.PROCESSING] whose [OutboxMessage.processedAt]
     *    is older than [stuckBefore] (crash-recovery for messages whose
     *    publisher died after the row was marked PROCESSING but before the
     *    publishing completed).
     *
     * Implementations MUST be called within an active transaction so the
     * pessimistic lock survives until the caller updates each row.
     */
    fun lockBatchForDispatch(
        maxRetries: Int,
        retriableBefore: Instant,
        stuckBefore: Instant,
        batchSize: Int,
    ): List<OutboxMessage>
}
