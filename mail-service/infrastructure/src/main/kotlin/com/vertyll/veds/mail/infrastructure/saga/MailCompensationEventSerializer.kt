package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.saga.model.MailCompensationCommand
import com.vertyll.veds.mail.saga.DeleteEmailLogAction
import com.vertyll.veds.mail.saga.LogEmailCompensationAction
import com.vertyll.veds.mail.saga.LogTemplateCompensationAction
import com.vertyll.veds.mail.saga.SagaCompensationEvent
import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer

/**
 * Outbound side of the Anti-Corruption Layer for mail compensation
 * events. Mirrors the tagged union declared in
 * `contracts/mail-service/saga-compensation-mail/v1/saga-compensation.avsc`.
 */
internal class MailCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer<MailCompensationCommand> {
    override fun serialize(
        sagaId: String,
        stepId: Long?,
        command: MailCompensationCommand,
    ): ByteArray {
        val action: Any =
            when (command) {
                is MailCompensationCommand.LogEmailCompensation ->
                    LogEmailCompensationAction
                        .newBuilder()
                        .setEmailId(command.emailId)
                        .setTo(command.to)
                        .build()
                is MailCompensationCommand.DeleteEmailLog ->
                    DeleteEmailLogAction
                        .newBuilder()
                        .setLogId(command.logId)
                        .build()
                is MailCompensationCommand.LogTemplateCompensation ->
                    LogTemplateCompensationAction
                        .newBuilder()
                        .setTemplateName(command.templateName)
                        .build()
            }
        val record =
            SagaCompensationEvent
                .newBuilder()
                .setSagaId(sagaId)
                .setStepId(stepId)
                .setAction(action)
                .build()
        return avroPayloadSerializer.serialize(topic, record)
    }
}
