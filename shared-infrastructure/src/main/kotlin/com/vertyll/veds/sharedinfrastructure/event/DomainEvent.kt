package com.vertyll.veds.sharedinfrastructure.event

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType",
    visible = true,
)
interface DomainEvent {
    val eventId: String
    val timestamp: Instant
    val eventType: String
    val sagaId: String?

    companion object {
        fun generateEventId(): String = UUID.randomUUID().toString()

        fun now(): Instant = Instant.now()
    }
}
