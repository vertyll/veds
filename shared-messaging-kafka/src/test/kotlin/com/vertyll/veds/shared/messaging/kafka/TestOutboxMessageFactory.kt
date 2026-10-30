package com.vertyll.veds.shared.messaging.kafka

import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessage
import com.vertyll.veds.shared.messaging.kafka.contract.OutboxMessageFactory

internal class TestOutboxMessageFactory : OutboxMessageFactory {
    private var counter = 0

    override fun create(
        topic: String,
        key: String,
        payload: ByteArray,
        sagaId: String?,
        eventId: String?,
    ): OutboxMessage =
        testOutboxMessage(
            eventId = eventId ?: "generated-${counter++}",
            topic = topic,
            key = key,
            payload = payload,
            sagaId = sagaId,
        )
}
