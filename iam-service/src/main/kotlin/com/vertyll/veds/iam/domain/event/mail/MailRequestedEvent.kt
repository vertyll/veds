package com.vertyll.veds.iam.domain.event.mail

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.vertyll.veds.sharedinfrastructure.event.DomainEvent
import java.time.Instant

@JsonTypeName("MAIL_REQUESTED")
data class MailRequestedEvent
    @JsonCreator
    constructor(
        @JsonProperty("eventId")
        override val eventId: String = DomainEvent.generateEventId(),
        @JsonProperty("timestamp")
        override val timestamp: Instant = DomainEvent.now(),
        @JsonProperty("eventType")
        override val eventType: String = EventType.MAIL_REQUESTED.value,
        @JsonProperty("to")
        val to: String,
        @JsonProperty("subject")
        val subject: String,
        @JsonProperty("templateName")
        val templateName: String,
        @JsonProperty("variables")
        val variables: Map<String, String>,
        @JsonProperty("replyTo")
        val replyTo: String? = null,
        @JsonProperty("priority")
        val priority: Int = 0,
        @JsonProperty("sagaId")
        override val sagaId: String? = null,
    ) : DomainEvent {
        constructor() : this(
            eventId = DomainEvent.generateEventId(),
            timestamp = DomainEvent.now(),
            eventType = EventType.MAIL_REQUESTED.value,
            to = "",
            subject = "",
            templateName = "",
            variables = emptyMap(),
        )
    }

