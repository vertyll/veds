package com.vertyll.veds.sharedinfrastructure.kafka

/**
 * Enum class representing the names of Kafka topics used in the application.
 */
enum class KafkaTopicNames(
    val value: String,
) {
    // Mail service topics
    MAIL_REQUESTED("mail-requested"),
    MAIL_SENT("mail-sent"),
    MAIL_FAILED("mail-failed"),

    // Saga topics
    SAGA_COMPENSATION("saga-compensation"),
    ;

    companion object {
        fun fromString(value: String): KafkaTopicNames? = KafkaTopicNames.entries.find { it.value == value }
    }
}
