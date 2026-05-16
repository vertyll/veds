package com.vertyll.veds.template.infrastructure.saga

import com.vertyll.veds.sharedinfrastructure.avro.AvroPayloadDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.CompensationCommandDeserializer
import com.vertyll.veds.sharedinfrastructure.saga.service.DecodedCompensationEvent
import com.vertyll.veds.template.application.saga.model.TemplateCompensationCommand
import com.vertyll.veds.template.saga.DeleteTemplateAction
import com.vertyll.veds.template.saga.LogTemplateCompensationAction
import com.vertyll.veds.template.saga.SagaCompensationEvent

/**
 * Anti-Corruption Layer (DDD) translating raw Avro bytes received on the
 * `saga-compensation-template` topic into the application-layer sealed
 * [TemplateCompensationCommand] hierarchy.
 *
 * This is the **only** place where Avro generated types meet the
 * template domain — the application layer therefore stays free of Avro,
 * Jackson, Kafka and stringly-typed dispatch.
 *
 * Mirrors the tagged union declared in
 * `template-contracts/avro/saga-compensation-template/v1/saga-compensation.avsc`.
 * Each branch is exhaustive — adding a new compensation action without
 * updating the translator becomes a compile-time error.
 */
internal class AvroTemplateCompensationCommandTranslator(
    private val avroPayloadDeserializer: AvroPayloadDeserializer,
    private val topic: String,
) : CompensationCommandDeserializer<TemplateCompensationCommand> {
    override fun deserialize(payload: ByteArray): DecodedCompensationEvent<TemplateCompensationCommand> {
        val record = avroPayloadDeserializer.deserialize(topic, payload) as SagaCompensationEvent
        val command =
            when (val action = record.action) {
                is DeleteTemplateAction ->
                    TemplateCompensationCommand.DeleteTemplate(templateId = action.templateId.toString())
                is LogTemplateCompensationAction ->
                    TemplateCompensationCommand.LogTemplateCompensation(templateId = action.templateId.toString())
                else ->
                    error(
                        "Unknown compensation action type on saga-compensation-template: ${action?.javaClass?.name}",
                    )
            }
        return DecodedCompensationEvent(
            sagaId = record.sagaId.toString(),
            stepId = record.stepId,
            command = command,
        )
    }
}
