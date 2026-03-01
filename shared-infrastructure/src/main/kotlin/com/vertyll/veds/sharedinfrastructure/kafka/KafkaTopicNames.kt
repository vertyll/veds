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

    // Saga topics — per-service compensation topics are defined
    // locally in each microservice, not here.
    ;

    /**
     * Compile-time constants for `@KafkaListener(topics = [...])`.
     */
    object Topics {
        const val MAIL_REQUESTED = "mail-requested"
        const val MAIL_SENT = "mail-sent"
        const val MAIL_FAILED = "mail-failed"
    }

    companion object {
        fun fromString(value: String): KafkaTopicNames? = KafkaTopicNames.entries.find { it.value == value }
    }
}
