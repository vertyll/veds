package com.vertyll.veds.mail.application.service

import com.vertyll.veds.mail.application.saga.port.SagaProcessPort
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.sharedinfrastructure.event.mail.MailFailedEvent
import com.vertyll.veds.sharedinfrastructure.event.mail.MailSentEvent
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmailSagaService(
    private val sagaProcess: SagaProcessPort,
    private val emailService: EmailService,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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
            sagaProcess.startSaga(
                sagaType = "EMAIL_SENDING",
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
            )

        try {
            sagaProcess.recordSagaStep(
                sagaId = sagaId,
                stepName = "PROCESS_TEMPLATE",
                status = SagaStepStatus.COMPLETED,
                payload = mapOf("templateName" to template.templateName, "variables" to variables),
            )

            val success = emailService.sendEmail(to, subject, template, variables, replyTo)

            val status = if (success) SagaStepStatus.COMPLETED else SagaStepStatus.FAILED
            val payload =
                if (success) {
                    mapOf("to" to to, "subject" to subject, "emailId" to sagaId)
                } else {
                    mapOf("to" to to, "subject" to subject, "error" to "Failed to send email")
                }

            sagaProcess.recordSagaStep(sagaId, "SEND_EMAIL", status, payload)

            if (success) {
                sagaProcess.markSagaCompleted(sagaId)
            }

            publishFeedbackEvent(
                success,
                to,
                subject,
                originSagaId,
                originalEventId ?: sagaId,
                if (!success) "Failed to send email" else null,
            )

            return success
        } catch (e: Exception) {
            logger.error("Error in email saga: ${e.message}", e)
            sagaProcess.recordSagaStep(sagaId, "SEND_EMAIL", SagaStepStatus.FAILED, mapOf("error" to e.message))
            publishFeedbackEvent(false, to, subject, originSagaId, originalEventId ?: sagaId, e.message ?: "Unknown error")
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
        if (originSagaId == null) return

        try {
            if (success) {
                val event = MailSentEvent(to = to, subject = subject, originalEventId = originalEventId, sagaId = originSagaId)
                kafkaOutboxProcessor.saveOutboxMessage(KafkaTopicNames.MAIL_SENT, originSagaId, event, originSagaId, event.eventId)
            } else {
                val event =
                    MailFailedEvent(
                        to = to,
                        subject = subject,
                        originalEventId = originalEventId,
                        error = error ?: "Unknown error",
                        sagaId = originSagaId,
                    )
                kafkaOutboxProcessor.saveOutboxMessage(KafkaTopicNames.MAIL_FAILED, originSagaId, event, originSagaId, event.eventId)
            }
        } catch (e: Exception) {
            logger.error("Failed to publish feedback event for saga $originSagaId", e)
        }
    }
}
