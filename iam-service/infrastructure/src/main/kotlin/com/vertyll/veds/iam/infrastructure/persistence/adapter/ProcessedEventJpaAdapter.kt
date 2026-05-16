package com.vertyll.veds.iam.infrastructure.persistence.adapter

import com.vertyll.veds.iam.infrastructure.persistence.entity.ProcessedEventJpaEntity
import com.vertyll.veds.iam.infrastructure.persistence.repository.ProcessedEventJpaRepository
import com.vertyll.veds.sharedinfrastructure.kafka.contract.ProcessedEvent
import com.vertyll.veds.sharedinfrastructure.kafka.contract.ProcessedEventFactory
import com.vertyll.veds.sharedinfrastructure.kafka.contract.ProcessedEventRepositoryPort
import org.springframework.stereotype.Component

@Component
internal class ProcessedEventJpaAdapter(
    private val repository: ProcessedEventJpaRepository,
) : ProcessedEventRepositoryPort,
    ProcessedEventFactory {
    override fun insert(processedEvent: ProcessedEvent): ProcessedEvent {
        val entity =
            processedEvent as? ProcessedEventJpaEntity
                ?: ProcessedEventJpaEntity(
                    eventId = processedEvent.eventId,
                    consumerGroup = processedEvent.consumerGroup,
                    processedAt = processedEvent.processedAt,
                )
        return repository.saveAndFlush(entity)
    }

    override fun exists(
        eventId: String,
        consumerGroup: String,
    ): Boolean = repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)

    override fun create(
        eventId: String,
        consumerGroup: String,
    ): ProcessedEvent = ProcessedEventJpaEntity(eventId = eventId, consumerGroup = consumerGroup)
}
