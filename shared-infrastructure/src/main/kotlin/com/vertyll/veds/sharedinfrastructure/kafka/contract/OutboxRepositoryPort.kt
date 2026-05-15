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

    fun findMessagesToProcess(
        status: OutboxStatus,
        maxRetries: Int,
        minRetryTime: Instant,
    ): List<OutboxMessage>
}
