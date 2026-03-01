package com.vertyll.veds.sharedinfrastructure.event.mail

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.vertyll.veds.sharedinfrastructure.event.DomainEvent
import com.vertyll.veds.sharedinfrastructure.event.EventType
import java.time.Instant

@JsonTypeName("MAIL_SENT")
data class MailSentEvent
    @JsonCreator
    constructor(
        @JsonProperty("eventId")
        override val eventId: String = DomainEvent.generateEventId(),
        @JsonProperty("timestamp")
        override val timestamp: Instant = DomainEvent.now(),
        @JsonProperty("eventType")
        override val eventType: String = EventType.MAIL_SENT.value,
        @JsonProperty("to")
        val to: String,
        @JsonProperty("subject")
        val subject: String,
        @JsonProperty("originalEventId")
        val originalEventId: String,
        @JsonProperty("sagaId")
        override val sagaId: String? = null,
    ) : DomainEvent
