package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.port.inbound.EmailSagaUseCase
import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.application.port.outbound.MailFeedbackEventPublisherPort
import com.vertyll.veds.mail.application.port.outbound.UseCaseLogger
import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.application.saga.model.SagaTypes
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStepStatus

private const val SUBJECT_KEY = "subject"

class EmailSagaService(
    private val sagaProcess: SagaProcessPort,
    private val emailService: EmailUseCase,
    private val mailFeedbackPublisher: MailFeedbackEventPublisherPort,
    private val logger: UseCaseLogger,
) : EmailSagaUseCase {
    override fun sendEmailWithSaga(
        to: String,
        templateName: String,
        variables: Map<String, String>,
        replyTo: String?,
        originSagaId: String?,
        originalEventId: String?,
    ): Boolean {
        val template = EmailTemplate.fromTemplateName(templateName)
        if (template == null) {
            val subject = templateName
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

        val subject = template.subject

        val sagaId =
            sagaProcess
                .startSaga(
                    sagaType = SagaTypes.EMAIL_SENDING,
                    payload =
                        mapOf(
                            "to" to to,
                            SUBJECT_KEY to subject,
                            "templateName" to templateName,
                            "variables" to variables,
                            "replyTo" to replyTo,
                            "originSagaId" to originSagaId,
                            "originalEventId" to originalEventId,
                        ),
                ).id

        val outcome =
            try {
                sagaProcess.recordSagaStep(
                    sagaId = sagaId,
                    stepName = SagaStepNames.PROCESS_TEMPLATE,
                    status = SagaStepStatus.COMPLETED,
                    payload = mapOf("templateName" to templateName, "variables" to variables),
                )

                val success = emailService.sendEmail(to, template, variables, replyTo)

                val status = if (success) SagaStepStatus.COMPLETED else SagaStepStatus.FAILED
                val stepPayload =
                    if (success) {
                        mapOf("to" to to, SUBJECT_KEY to subject, "emailId" to sagaId)
                    } else {
                        mapOf("to" to to, SUBJECT_KEY to subject, "error" to "Failed to send email")
                    }

                sagaProcess.recordSagaStep(sagaId, SagaStepNames.SEND_EMAIL, status, stepPayload)

                if (success) {
                    sagaProcess.markSagaCompleted(sagaId)
                    EmailOutcome.Sent
                } else {
                    EmailOutcome.Failed("Failed to send email")
                }
            } catch (e: Exception) {
                logger.error("Error in email saga: ${e.message}", e)
                sagaProcess.recordSagaStep(sagaId, SagaStepNames.SEND_EMAIL, SagaStepStatus.FAILED, mapOf("error" to e.message))
                EmailOutcome.Failed(e.message ?: e.javaClass.name)
            }

        originSagaId?.let {
            publishFeedback(it, SentEmail(to, subject, originalEventId ?: sagaId), outcome)
        }

        return outcome == EmailOutcome.Sent
    }

    private fun publishFeedback(
        originSagaId: String,
        email: SentEmail,
        outcome: EmailOutcome,
    ) {
        runCatching {
            when (outcome) {
                EmailOutcome.Sent -> {
                    mailFeedbackPublisher.publishMailSent(
                        originSagaId = originSagaId,
                        to = email.to,
                        subject = email.subject,
                        originalEventId = email.originalEventId,
                    )
                }

                is EmailOutcome.Failed -> {
                    mailFeedbackPublisher.publishMailFailed(
                        originSagaId = originSagaId,
                        to = email.to,
                        subject = email.subject,
                        originalEventId = email.originalEventId,
                        error = outcome.reason,
                    )
                }
            }
        }.onFailure { e ->
            logger.error("Failed to publish feedback event for saga $originSagaId", e)
        }
    }
}

private data class SentEmail(
    val to: String,
    val subject: String,
    val originalEventId: String,
)

private sealed interface EmailOutcome {
    data object Sent : EmailOutcome

    data class Failed(
        val reason: String,
    ) : EmailOutcome
}
