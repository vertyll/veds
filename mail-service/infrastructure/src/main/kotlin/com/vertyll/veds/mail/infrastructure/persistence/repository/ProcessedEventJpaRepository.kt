package com.vertyll.veds.mail.infrastructure.persistence.repository

import com.vertyll.veds.mail.infrastructure.persistence.entity.ProcessedEventJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProcessedEventJpaRepository : JpaRepository<ProcessedEventJpaEntity, Long> {
    fun existsByEventIdAndConsumerGroup(
        eventId: String,
        consumerGroup: String,
    ): Boolean
}
