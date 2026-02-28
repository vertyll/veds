package com.vertyll.veds.sharedinfrastructure.event

enum class EventSource(
    val value: String,
) {
    IAM_SERVICE("IAM_SERVICE"),
    MAIL_SERVICE("MAIL_SERVICE"),
    ;

    companion object {
        fun fromString(value: String): EventSource? = EventSource.entries.find { it.value == value }
    }
}
