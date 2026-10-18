package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.shared.messaging.avro.AvroPayloadDeserializer
import com.vertyll.veds.shared.saga.engine.CompensationCommandDeserializer
import com.vertyll.veds.shared.saga.engine.DecodedCompensationEvent
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.saga.DeleteTemplateAction
import com.vertyll.veds.template.saga.LogTemplateCompensationAction
import com.vertyll.veds.template.saga.SagaCompensationEvent

internal class AvroTemplateCompensationCommandTranslator(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationCommandDeserializer<TemplateCompensationCommand> {
    override fun deserialize(payload: ByteArray): DecodedCompensationEvent<TemplateCompensationCommand> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as SagaCompensationEvent
        val command =
            when (val action = record.action) {
                is DeleteTemplateAction -> {
                    TemplateCompensationCommand.DeleteTemplate(templateId = action.templateId.toString())
                }

                is LogTemplateCompensationAction -> {
                    TemplateCompensationCommand.LogTemplateCompensation(templateId = action.templateId.toString())
                }

                else -> {
                    error(
                        "Unknown compensation action type on saga-compensation-template: ${action?.javaClass?.name}",
                    )
                }
            }
        return DecodedCompensationEvent(
            sagaId = record.sagaId.toString(),
            stepId = record.stepId,
            command = command,
        )
    }
}
