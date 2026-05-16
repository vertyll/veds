package com.vertyll.veds.mail.application.saga.model

/**
 * Application-layer compensation commands for the mail bounded context.
 *
 * A `sealed interface` mirroring the Avro tagged union sitting on the
 * `saga-compensation-mail` topic
 * (`contracts/mail-service/saga-compensation-mail/v1/saga-compensation.avsc`).
 *
 * NOTE: mail-service does NOT currently host an inbound listener for
 * its compensation topic — a sent email cannot be unsent, and email
 * logs are typically retained for audit. The publisher is wired so the
 * choreography pattern stays uniform across services and so future
 * reversible mail operations (e.g. cancelling a queued bulk send) can
 * be added without changes to the integration contract.
 */
sealed interface MailCompensationCommand {
    /**
     * No-op compensation marker for a `SendEmail` step — recorded for
     * audit, since the email itself cannot be retracted.
     */
    data class LogEmailCompensation(
        val emailId: String,
        val to: String,
    ) : MailCompensationCommand

    /**
     * Compensates a `RecordEmailLog` step — removes the local email log
     * entry that the failed flow created.
     */
    data class DeleteEmailLog(
        val logId: Long,
    ) : MailCompensationCommand

    /**
     * Audit-only compensation for a `TemplateUpdate` step.
     */
    data class LogTemplateCompensation(
        val templateName: String,
    ) : MailCompensationCommand
}
