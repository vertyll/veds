package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadSerializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationEventSerializer
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.saga.DeleteTemplateAction
import com.vertyll.veds.template.saga.LogTemplateCompensationAction
import com.vertyll.veds.template.saga.SagaCompensationEvent

/**
 * Outbound side of the Anti-Corruption Layer for template compensation
 * events.
 *
 * Translates a typed application-layer [TemplateCompensationCommand]
 * into a generated Avro [SagaCompensationEvent] SpecificRecord and
 * delegates to the Confluent Avro serializer (which registers the
 * schema in Schema Registry).
 *
 * Mirrors the tagged union declared in
 * `template-contracts/avro/saga-compensation-template/v1/saga-compensation.avsc`
 * — exhaustive `when` over the sealed hierarchy means a new
 * compensation action becomes a compile-time error here too.
 */
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
                is TemplateCompensationCommand.DeleteTemplate ->
                    DeleteTemplateAction.newBuilder().setTemplateId(command.templateId).build()
                is TemplateCompensationCommand.LogTemplateCompensation ->
                    LogTemplateCompensationAction.newBuilder().setTemplateId(command.templateId).build()
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
