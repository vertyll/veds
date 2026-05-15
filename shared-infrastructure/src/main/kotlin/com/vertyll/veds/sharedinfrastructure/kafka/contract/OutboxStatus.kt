package com.vertyll.veds.sharedinfrastructure.kafka.contract

/**
 * Persistence-agnostic status of an outbox message.
 */
enum class OutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}
