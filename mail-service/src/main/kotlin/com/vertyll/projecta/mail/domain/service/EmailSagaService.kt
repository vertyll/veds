package com.vertyll.projecta.mail.domain.service

import com.vertyll.projecta.mail.domain.model.enums.EmailTemplate
import com.vertyll.projecta.mail.domain.model.enums.SagaStepNames
import com.vertyll.projecta.mail.domain.model.enums.SagaStepStatus
import com.vertyll.projecta.mail.domain.model.enums.SagaTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service that handles email sending with saga pattern
 */
@Service
class EmailSagaService(
    private val sagaManager: SagaManager,
    private val emailService: EmailService,
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
            return false
        }
    }

    /**
     * Processes a template and sends an email with batch processing saga tracking
     */
    @Transactional
    fun processEmailBatch(
        recipients: List<String>,
        subject: String,
        template: EmailTemplate,
        commonVariables: Map<String, String>,
        specificVariables: Map<String, Map<String, String>> = emptyMap(),
        replyTo: String? = null,
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

            sagaManager.recordSagaStep(
                sagaId = sagaId,
                stepName = SagaStepNames.SEND_EMAIL,
                status = if (results.values.all { it }) SagaStepStatus.COMPLETED else SagaStepStatus.PARTIALLY_COMPLETED,
                payload =
                    mapOf(
                        "recipients" to recipients,
                        "subject" to subject,
                        "results" to results,
                    ),
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
            return recipients.associateWith { false }
        }
    }
}
