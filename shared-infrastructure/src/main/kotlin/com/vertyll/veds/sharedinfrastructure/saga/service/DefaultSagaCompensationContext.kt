package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

/**
 * Default [SagaCompensationContext] implementation shared by [SagaEngine]
 * and [SagaCompensationRunner].
 *
 * Extracted out of the engine so the runner can be constructed without a
 * circular dependency on the engine: both collaborators receive the same
 * context instance from configuration.
 */
class DefaultSagaCompensationContext<TCommand : Any>(
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val compensationEventSerializer: CompensationEventSerializer<TCommand>,
    private val compensationTopic: String,
    private val objectMapper: ObjectMapper,
) : SagaCompensationContext<TCommand> {
    override fun publishCompensationEvent(
        sagaId: String,
        stepId: Long?,
        command: TCommand,
    ) {
        val payload =
            compensationEventSerializer.serialize(
                sagaId = sagaId,
                stepId = stepId,
                command = command,
            )
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = compensationTopic,
            key = sagaId,
            payload = payload,
            sagaId = sagaId,
        )
    }

    override fun readStepPayload(payload: String?): Map<String, Any?> {
        if (payload.isNullOrBlank()) return emptyMap()
        return objectMapper.readValue(payload, object : TypeReference<Map<String, Any?>>() {})
    }
}
