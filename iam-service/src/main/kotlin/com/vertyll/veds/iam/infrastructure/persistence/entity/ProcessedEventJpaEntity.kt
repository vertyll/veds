package com.vertyll.veds.iam.infrastructure.persistence.entity

import com.vertyll.veds.sharedinfrastructure.kafka.entity.BaseProcessedEvent
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "processed_event",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_processed_event_event_id_consumer",
            columnNames = ["event_id", "consumer_group"],
        ),
    ],
)
internal class ProcessedEventJpaEntity(
    id: Long? = null,
    eventId: String,
    consumerGroup: String,
    processedAt: Instant = Instant.now(),
) : BaseProcessedEvent(
        id = id,
        eventId = eventId,
        consumerGroup = consumerGroup,
        processedAt = processedAt,
    )
