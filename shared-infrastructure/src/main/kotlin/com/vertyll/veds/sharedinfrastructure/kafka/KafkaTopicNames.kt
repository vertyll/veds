package com.vertyll.veds.sharedinfrastructure.kafka

/**
 * Enum class representing the names of Kafka topics used in the application.
 *
 * The [Topics] object contains `const val` topic names for use in
 * `@KafkaListener` annotations (which require compile-time constants).
 *
 * Usage:
 * ```
 * @KafkaListener(topics = [KafkaTopicNames.Topics.MAIL_SENT])
 * ```
 */
enum class KafkaTopicNames(
    val value: String,
) {
    // Mail service topics
    MAIL_REQUESTED(Topics.MAIL_REQUESTED),
    MAIL_SENT(Topics.MAIL_SENT),
    MAIL_FAILED(Topics.MAIL_FAILED),

    // Saga topics
    SAGA_COMPENSATION(Topics.SAGA_COMPENSATION),
    ;

    /**
     * Compile-time constants for `@KafkaListener(topics = [...])`.
     */
    object Topics {
        const val MAIL_REQUESTED = "mail-requested"
        const val MAIL_SENT = "mail-sent"
        const val MAIL_FAILED = "mail-failed"
        const val SAGA_COMPENSATION = "saga-compensation"
    }

    companion object {
        fun fromString(value: String): KafkaTopicNames? = KafkaTopicNames.entries.find { it.value == value }
    }
}
