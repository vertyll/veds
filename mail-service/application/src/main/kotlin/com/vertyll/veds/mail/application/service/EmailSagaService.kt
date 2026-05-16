package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailSagaUseCase
import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.application.port.out.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.application.port.out.SagaProcessPort
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.application.saga.model.SagaTypes
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service orchestrating the mail-delivery use case (driven by `mail-requested`).
 *
 * Pure application layer:
 *   - knows the domain ([EmailTemplate], [EmailService], saga state machine);
 *   - knows _what_ feedback events to publish — but **not** _how_, that goes through
 *     [MailFeedbackEventPublisherPort] (Kafka / Avro / Outbox live behind the port).
 *
 * Inbound entry point used by the Kafka adapter (the Kafka inbound adapter).
 */
@Service
internal class EmailSagaService(
    private val sagaProcess: SagaProcessPort,
    private val emailService: EmailUseCase,
    private val mailFeedbackPublisher: MailFeedbackEventPublisherPort,
) : EmailSagaUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sendEmailWithSaga(
        to: String,
        subject: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String?,
        originSagaId: String?,
        originalEventId: String?,
    ): Boolean {
        val template = EmailTemplate.fromTemplateName(templateName)
        if (template == null) {
            logger.error("Invalid template name: {}. Email will not be sent.", templateName)
            if (originSagaId != null) {
                mailFeedbackPublisher.publishMailFailed(
                    originSagaId = originSagaId,
                    to = to,
                    subject = subject,
                    originalEventId = originalEventId ?: originSagaId,
                    error = "Invalid template name: $templateName",
                )
            }
            return false
        }

        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.EMAIL_SENDING,
                    payload =
                        mapOf(
                            "to" to to,
                            "subject" to subject,
                            "templateName" to templateName,
                            "variables" to variables,
                            "replyTo" to replyTo,
                            "originSagaId" to originSagaId,
                            "originalEventId" to originalEventId,
                        ),
                ).id

        try {
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.PROCESS_TEMPLATE,
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("templateName" to templateName, "variables" to variables),
            )

            val success = emailService.sendEmail(to, subject, template, variables, replyTo)

            val status = if (success) SagaStepStatus.COMPLETED else SagaStepStatus.FAILED
            val stepPayload =
                if (success) {
                    mapOf("to" to to, "subject" to subject, "emailId" to sagaId)
                } else {
                    mapOf("to" to to, "subject" to subject, "error" to "Failed to send email")
                }

            sagaProcess.recordSagaStep(sagaId, SagaStepNames.SEND_EMAIL, status, stepPayload)

            if (success) {
                sagaProcess.markSagaCompleted(sagaId)
            }

            publishFeedback(
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
            sagaProcess.recordSagaStep(sagaId, SagaStepNames.SEND_EMAIL, SagaStepStatus.FAILED, mapOf("error" to e.message))
            publishFeedback(
                success = false,
                to = to,
                subject = subject,
                originSagaId = originSagaId,
                originalEventId = originalEventId ?: sagaId,
                error = e.message ?: "Unknown error",
            )
            return false
        }
    }

    private fun publishFeedback(
        success: Boolean,
        to: String,
        subject: String,
        originSagaId: String?,
        originalEventId: String,
        error: String?,
    ) {
        if (originSagaId == null) return
        runCatching {
            if (success) {
                mailFeedbackPublisher.publishMailSent(
                    originSagaId = originSagaId,
                    to = to,
                    subject = subject,
                    originalEventId = originalEventId,
                )
            } else {
                mailFeedbackPublisher.publishMailFailed(
                    originSagaId = originSagaId,
                    to = to,
                    subject = subject,
                    originalEventId = originalEventId,
                    error = error ?: "Unknown error",
                )
            }
        }.onFailure { e ->
            logger.error("Failed to publish feedback event for saga $originSagaId", e)
        }
    }
}
