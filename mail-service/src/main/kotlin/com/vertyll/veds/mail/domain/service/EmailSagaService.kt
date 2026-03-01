package com.vertyll.veds.mail.domain.service

import com.vertyll.veds.mail.domain.model.enums.EmailTemplate
import com.vertyll.veds.mail.domain.model.enums.SagaStepNames
import com.vertyll.veds.mail.domain.model.enums.SagaTypes
import com.vertyll.veds.sharedinfrastructure.event.mail.MailFailedEvent
import com.vertyll.veds.sharedinfrastructure.event.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service that handles email sending with a saga pattern
 */
@Service
class EmailSagaService(
    private val sagaManager: SagaManager,
    private val emailService: EmailService,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Sends an email with saga tracking
     */
    @Transactional
    fun sendEmailWithSaga(
        to: String,
        subject: String,
        template: EmailTemplate,
        variables: Map<String, String>,
        replyTo: String? = null,
        originSagaId: String? = null,
        originalEventId: String? = null,
    ): Boolean {
        val sagaId =
            sagaManager
                .startSaga(
                    sagaType = SagaTypes.EMAIL_SENDING,
                    payload =
                        mapOf(
                            "to" to to,
                            "subject" to subject,
                            "templateName" to template.templateName,
                            "variables" to variables,
                            "replyTo" to replyTo,
                            "originSagaId" to originSagaId,
                            "originalEventId" to originalEventId,
                        ),
                ).id

        try {
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "templateName" to template.templateName,
                        "variables" to variables,
                    ),
            )

            val success = emailService.sendEmail(to, subject, template, variables, replyTo)

            val status = if (success) SagaStepStatus.COMPLETED else SagaStepStatus.FAILED
            val payload =
                if (success) {
                    mapOf(
                        "to" to to,
                        "subject" to subject,
                        "emailId" to sagaId,
                    )
                } else {
                    mapOf(
                        "to" to to,
                        "subject" to subject,
                        "error" to "Failed to send email",
                    )
                }

            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = status,
                payload = payload,
            )

            if (success) {
                sagaManager.completeSaga(sagaId)
            }

            publishFeedbackEvent(
                success = success,
                to = to,
                subject = subject,
                originSagaId = originSagaId,
                originalEventId = originalEventId ?: sagaId,
                error = if (!success) "Failed to send email" else null,
            )

            return success
        } catch (e: Exception) {
            logger.error("Error in email saga: ${e.message}", e)
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = SagaStepStatus.FAILED,
                payload =
                    mapOf(
                        "error" to e.message,
                    ),
            )

            publishFeedbackEvent(
                success = false,
                to = to,
                subject = subject,
                originSagaId = originSagaId,
                originalEventId = originalEventId ?: sagaId,
                error = e.message ?: "Unknown error during email sending",
            )

            return false
        }
    }

    private fun publishFeedbackEvent(
        success: Boolean,
        to: String,
        subject: String,
        originSagaId: String?,
        originalEventId: String,
        error: String?,
    ) {
        if (originSagaId == null) {
            logger.debug("No origin saga ID provided — skipping feedback event for mail to: {}", to)
            return
        }

        try {
            if (success) {
                val event =
                    MailSentEvent(
                        to = to,
                        subject = subject,
                        originalEventId = originalEventId,
                        sagaId = originSagaId,
                    )
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.MAIL_SENT,
                    key = originSagaId,
                    payload = event,
                    sagaId = originSagaId,
                    eventId = event.eventId,
                )
                logger.info("Published MailSentEvent for saga: {}", originSagaId)
            } else {
                val event =
                    MailFailedEvent(
                        to = to,
                        subject = subject,
                        originalEventId = originalEventId,
                        error = error ?: "Unknown error",
                        sagaId = originSagaId,
                    )
                kafkaOutboxProcessor.saveOutboxMessage(
                    topic = KafkaTopicNames.MAIL_FAILED,
                    key = originSagaId,
                    payload = event,
                    sagaId = originSagaId,
                    eventId = event.eventId,
                )
                logger.info("Published MailFailedEvent for saga: {}", originSagaId)
            }
        } catch (e: Exception) {
            logger.error("Failed to publish feedback event for saga {}: {}", originSagaId, e.message, e)
        }
    }

    /**
     * Processes a template and sends an email with batch processing saga tracking.
     *
     * @param originSagaId  Optional saga ID from the calling service. When present,
     *                      a [MailSentEvent] or [MailFailedEvent] feedback event
     *                      is published so the caller can track the outcome.
     * @param originalEventId  Optional event ID for correlation.
     */
    @Transactional
    fun processEmailBatch(
        recipients: List<String>,
        subject: String,
        template: EmailTemplate,
        commonVariables: Map<String, String>,
        specificVariables: Map<String, Map<String, String>> = emptyMap(),
        replyTo: String? = null,
        originSagaId: String? = null,
        originalEventId: String? = null,
    ): Map<String, Boolean> {
        val sagaId =
            sagaManager
                .startSaga(
                    sagaType = SagaTypes.EMAIL_BATCH_PROCESSING,
                    payload =
                        mapOf(
                            "recipients" to recipients,
                            "subject" to subject,
                            "templateName" to template.templateName,
                            "commonVariables" to commonVariables,
                            "specificVariables" to specificVariables,
                            "replyTo" to replyTo,
                        ),
                ).id

        val results = mutableMapOf<String, Boolean>()

        try {
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "templateName" to template.templateName,
                        "variables" to commonVariables,
                    ),
            )

            recipients.forEach { recipient ->
                val recipientVariables = commonVariables.toMutableMap()
                specificVariables[recipient]?.let { recipientVariables.putAll(it) }

                val success = emailService.sendEmail(recipient, subject, template, recipientVariables, replyTo)
                results[recipient] = success
            }

            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.RECORD_EMAIL_LOG,
                status = SagaStepStatus.COMPLETED,
                payload =
                    mapOf(
                        "logId" to sagaId,
                        "results" to results,
                    ),
            )

            val allSucceeded = results.values.all { it }
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = if (allSucceeded) SagaStepStatus.COMPLETED else SagaStepStatus.PARTIALLY_COMPLETED,
                payload =
                    mapOf(
                        "recipients" to recipients,
                        "subject" to subject,
                        "results" to results,
                    ),
            )

            sagaManager.completeSaga(sagaId)

            publishFeedbackEvent(
                success = allSucceeded,
                to = recipients.joinToString(", "),
                subject = subject,
                originSagaId = originSagaId,
                originalEventId = originalEventId ?: sagaId,
                error = if (!allSucceeded) "Some recipients failed in batch" else null,
            )

            return results
        } catch (e: Exception) {
            logger.error("Error in email batch saga: ${e.message}", e)
            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = SagaStepStatus.FAILED,
                payload =
                    mapOf(
                        "error" to e.message,
                    ),
            )

            publishFeedbackEvent(
                success = false,
                to = recipients.joinToString(", "),
                subject = subject,
                originSagaId = originSagaId,
                originalEventId = originalEventId ?: sagaId,
                error = e.message ?: "Unknown error during batch email sending",
            )

            return recipients.associateWith { false }
        }
    }
}
