package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.shared.messaging.avro.AvroPayloadSerializer
import com.vertyll.veds.shared.saga.engine.CompensationEventSerializer
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.saga.DeleteTemplateAction
import com.vertyll.veds.template.saga.LogTemplateCompensationAction
import com.vertyll.veds.template.saga.SagaCompensationEvent

internal class TemplateCompensationEventSerializer(
    private val avroPayloadSerializer: AvroPayloadSerializer,
    private val topic: String,
) : CompensationEventSerializer<TemplateCompensationCommand> {
    override fun serialize(
        sagaId: String,
        stepId: Long?,
        command: TemplateCompensationCommand,
    ): ByteArray {
        val action: Any =
            when (command) {
                is TemplateCompensationCommand.DeleteTemplate -> {
                    DeleteTemplateAction.newBuilder().setTemplateId(command.templateId).build()
                }

                is TemplateCompensationCommand.LogTemplateCompensation -> {
                    LogTemplateCompensationAction.newBuilder().setTemplateId(command.templateId).build()
                }
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
