package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Engine for the **choreography** saga compensation flow.
 *
 * Uses composition instead of inheritance (replaces the previous
 * `BaseSagaCompensationService` Template Method): it depends on two narrow
 * hooks — [SagaCompensationStepFactory] (service-specific JPA step entity) and
 * [SagaCompensationHandler] (domain-specific compensation dispatch).
 *
 * Each microservice listens on its own dedicated Kafka topic
 * (e.g. `saga-compensation-iam`); the Kafka adapter is a thin `@KafkaListener`
 * that delegates a single call to [handleCompensationEvent].
 *
 * `open` solely because Spring's `@Transactional` requires CGLIB proxy when
 * the bean has no interface; this class is **not** designed for subclassing.
 */
open class SagaCompensationEngine<T : BaseSagaStep>(
    private val sagaStepRepository: BaseSagaStepRepository<T>,
    private val objectMapper: ObjectMapper,
    private val stepFactory: SagaCompensationStepFactory<T>,
    private val handler: SagaCompensationHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    open fun handleCompensationEvent(payload: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val event = objectMapper.readValue(payload, Map::class.java) as Map<String, Any?>
            val sagaId = event["sagaId"] as String
            val actionStr = event["action"] as String

            logger.info("Processing compensation action: {} for saga {}", actionStr, sagaId)

            handler.handle(sagaId, actionStr, event)

            recordCompensationStep(sagaId, event)
        } catch (e: Exception) {
            logger.error("Failed to process compensation event: ${e.message}", e)
        }
    }

    private fun recordCompensationStep(
        sagaId: String,
        event: Map<String, Any?>,
    ) {
        val stepId = event["stepId"] as? Number ?: return
        val step = sagaStepRepository.findById(stepId.toLong()).orElse(null) ?: return

        val compensationStep =
            stepFactory.createCompensationStep(
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
