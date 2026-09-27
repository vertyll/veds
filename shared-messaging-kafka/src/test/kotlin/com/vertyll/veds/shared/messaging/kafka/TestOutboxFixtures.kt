package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxStatus
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxEntity

/**
 * A row that behaves exactly like a persisted one, because it is one: the transitions come from
 * [OutboxEntity] itself, so a test can never pass against a state machine the services do not
 * actually use.
 */
internal fun testOutboxMessage(
    id: Long? = null,
    eventId: String = "event-1",
    topic: String = "project-created",
    key: String = "project-1",
    payload: ByteArray = byteArrayOf(1),
    status: OutboxStatus = OutboxStatus.PENDING,
    retryCount: Int = 0,
    sagaId: String? = null,
) = OutboxEntity(
    id = id,
    eventId = eventId,
    topic = topic,
    key = key,
    payload = payload,
    status = status,
    retryCount = retryCount,
    sagaId = sagaId,
)
