package com.vertyll.veds.sharedinfrastructure.event

enum class EventSource(
    val value: String,
) {
    IDENTITY_SERVICE("IDENTITY_SERVICE"),
    MAIL_SERVICE("MAIL_SERVICE"),
    ;

    companion object {
        fun fromString(value: String): EventSource? = EventSource.entries.find { it.value == value }
    }
}
