package com.vertyll.veds.sharedinfrastructure.kafka.entity

import com.vertyll.veds.sharedinfrastructure.kafka.contract.ProcessedEvent
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.Instant

/**
 * JPA `@MappedSuperclass` providing the column mapping for a row in the
 * per-service `processed_event` table used by the consumer-side
 * **idempotent receiver** pattern.
 *
 * Concrete per-service entities extend this class with their own `@Entity`
 * + `@Table(name = "processed_event")` annotations. The uniqueness of
 * `(event_id, consumer_group)` is enforced by a `UNIQUE` constraint in the
 * Flyway migration; the constraint violation is the signal that a duplicate
 * was received.
 */
@MappedSuperclass
abstract class BaseProcessedEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null,
    @Column(nullable = false)
    override var eventId: String,
    @Column(nullable = false)
    override var consumerGroup: String,
    @Column(nullable = false)
    override var processedAt: Instant = Instant.now(),
) : ProcessedEvent
