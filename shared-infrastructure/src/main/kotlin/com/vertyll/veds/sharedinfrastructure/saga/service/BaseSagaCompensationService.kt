package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.event.EventSource
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Base compensation handler for the **choreography** saga pattern.
 *
 * Each microservice extends this class and implements [processCompensation]
 * with domain-specific compensation logic. The base class takes care of:
 *
 * - Deserializing the Kafka message
 * - Filtering messages by [serviceSource] so that a service processes only
 *   its **own** compensation events (all services share the same
 *   `saga-compensation` topic)
 * - Persisting a compensation step record after successful processing
 * - Error handling and logging
 *
 * Subclasses must also implement [createCompensationStepEntity] to produce
 * the correct JPA entity type for the owning service.
 *
 * @param T The saga step entity type that extends [BaseSagaStep]
 */
abstract class BaseSagaCompensationService<T : BaseSagaStep>(
    protected val sagaStepRepository: BaseSagaStepRepository<T>,
    protected val objectMapper: ObjectMapper,
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Identifies this service so that only compensation events published
     * by the same service are processed. Messages from other services are
     * silently ignored.
     */
    protected abstract val serviceSource: EventSource

    /**
     * Creates a new compensation step entity of the concrete type [T].
     */
    protected abstract fun createCompensationStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        createdAt: Instant,
        completedAt: Instant?,
        compensationStepId: Long?,
    ): T

    /**
     * Domain-specific compensation logic.
     *
     * Called only for events whose `source` matches [serviceSource].
     * Implementations should dispatch on the `action` field.
     *
     * @param sagaId The saga being compensated
     * @param action The compensation action string (e.g. `DELETE_USER`)
     * @param event  The full deserialized event map
     */
    protected abstract fun processCompensation(
        sagaId: String,
        action: String,
        event: Map<String, Any?>,
    )

    /**
     * Listens for compensation events on the shared `saga-compensation` topic,
     * filters by [serviceSource], delegates to [processCompensation], and
     * records a compensation step.
     */
    @KafkaListener(topics = [KafkaTopicNames.Topics.SAGA_COMPENSATION])
    @Transactional
    open fun handleCompensationEvent(payload: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val event = objectMapper.readValue(payload, Map::class.java) as Map<String, Any?>
            val sagaId = event["sagaId"] as String
            val source = event["source"] as? String
            val actionStr = event["action"] as String

            if (source != null && source != serviceSource.value) {
                logger.debug(
                    "Ignoring compensation event from source '{}' (this service: {})",
                    source,
                    serviceSource.value,
                )
                return
            }

            logger.info("Processing compensation action: {} for saga {}", actionStr, sagaId)

            processCompensation(sagaId, actionStr, event)

            recordCompensationStep(sagaId, event)
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }

    /**
     * Persists a compensation step record linked to the original step.
     */
    private fun recordCompensationStep(
        sagaId: String,
        event: Map<String, Any?>,
    ) {
        val stepId = event["stepId"] as? Number ?: return
        val step = sagaStepRepository.findById(stepId.toLong()).orElse(null) ?: return

        val compensationStep =
            createCompensationStepEntity(
                sagaId = sagaId,
                stepName = "$COMPENSATION_PREFIX${step.stepName}",
                status = SagaStepStatus.COMPENSATED,
                createdAt = Instant.now(),
                completedAt = Instant.now(),
                compensationStepId = step.id,
            )
        sagaStepRepository.save(compensationStep)
    }

    companion object {
        const val COMPENSATION_PREFIX = "Compensate"
    }
}
