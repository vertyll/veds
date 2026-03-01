package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
 * - Persisting a compensation step record after successful processing
 * - Error handling and logging
 *
 * Each service listens on its own dedicated Kafka topic (e.g.
 * `saga-compensation-iam`), so no cross-service filtering is needed.
 * Subclasses must annotate their override of [handleCompensationEvent]
 * with `@KafkaListener` pointing to the service-specific topic.
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
     * Processes a compensation event from Kafka.
     *
     * Subclasses **must** override this method and annotate it with
     * `@KafkaListener(topics = [...])` pointing to the service-specific
     * compensation topic. The override should simply delegate:
     *
     * ```kotlin
     * @KafkaListener(topics = [KafkaTopicNames.Topics.SAGA_COMPENSATION_IAM])
     * override fun handleCompensationEvent(payload: String) = super.handleCompensationEvent(payload)
     * ```
     */
    @Transactional
    open fun handleCompensationEvent(payload: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val event = objectMapper.readValue(payload, Map::class.java) as Map<String, Any?>
            val sagaId = event["sagaId"] as String
            val actionStr = event["action"] as String

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
