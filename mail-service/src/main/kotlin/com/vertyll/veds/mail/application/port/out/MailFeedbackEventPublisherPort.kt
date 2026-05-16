package com.vertyll.veds.mail.application.port.out

/**
 * Outbound port for publishing mail-delivery feedback events back to the rest of the system.
 *
 * Technology-agnostic by design: the application layer must not know about Kafka / Avro / Outbox.
 * The Kafka + Avro implementation lives in `infrastructure/kafka/KafkaMailFeedbackEventPublisherAdapter`.
 */
interface MailFeedbackEventPublisherPort {
    fun publishMailSent(
        originSagaId: String,
        to: String,
        subject: String,
        originalEventId: String,
    )

    fun publishMailFailed(
        originSagaId: String,
        to: String,
        subject: String,
        originalEventId: String,
        error: String,
    )
}
