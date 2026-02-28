package com.vertyll.projecta.sharedinfrastructure.event

enum class EventType(
    val value: String,
) {
    // Mail events
    MAIL_REQUESTED("MAIL_REQUESTED"),
    MAIL_SENT("MAIL_SENT"),
    MAIL_FAILED("MAIL_FAILED"),
    ;

    companion object {
        fun fromString(value: String): EventType? = EventType.entries.find { it.value == value }
    }
}
