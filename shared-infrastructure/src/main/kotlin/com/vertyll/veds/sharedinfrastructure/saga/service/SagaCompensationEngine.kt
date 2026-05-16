package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStepRepositoryPort
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Engine for the **choreography** saga compensation flow.
 *
 * Depends only on the persistence-agnostic [SagaStepRepositoryPort] and two
 * collaborator hooks ([SagaCompensationStepFactory] and
 * [SagaCompensationHandler]) so it is decoupled from the underlying storage
 * technology (JPA today, others possible).
 */
open class SagaCompensationEngine<T : SagaStep<T>>(
    private val sagaStepRepository: SagaStepRepositoryPort<T>,
    private val compensationEventDeserializer: CompensationEventDeserializer,
    private val stepFactory: SagaCompensationStepFactory<T>,
    private val handler: SagaCompensationHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Entry point for inbound compensation events.
     *
     * Deserializes [payload] via [CompensationEventDeserializer], delegates
     * the domain action to [SagaCompensationHandler], and records a
     * `Compensate<originalStep>` audit row through
     * [SagaCompensationStepFactory]. Exceptions are logged and swallowed —
     * by the time we reach this method the originating saga is already in
     * a compensation flow; the watchdog will retry later if needed.
     */
    @Transactional
    open fun handleCompensationEvent(payload: ByteArray) {
        try {
            val event = compensationEventDeserializer.deserializeCompensationEvent(payload)
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
        val step = sagaStepRepository.findOneById(stepId.toLong()) ?: return

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
