package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.SagaStepStatus
import com.vertyll.veds.shared.saga.engine.persistence.SagaStepRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Engine for the **choreography** saga compensation flow.
 *
 * Depends only on the persistence-agnostic [SagaStepRepositoryPort] and
 * two collaborator hooks ([SagaCompensationStepFactory] and
 * [CompensationCommandHandler]) so it is decoupled from the underlying
 * storage technology (JPA today, others possible) and from the wire
 * format of compensation events.
 *
 * The type parameter [TCommand] is the service-local, strongly-typed
 * compensation command (typically a Kotlin `sealed interface` mirroring
 * the Avro tagged union). The engine never sees `Map<String, Any?>` or
 * stringly-typed action discriminators — the Anti-Corruption Layer that
 * decodes raw Kafka bytes into [TCommand] lives in each service's
 * infrastructure module (an implementation of
 * [CompensationCommandDeserializer]).
 *
 * Exceptions thrown by [CompensationCommandHandler] are intentionally
 * propagated so the inbound Kafka listener can trigger broker-level
 * retry / DLT routing — the [SagaWatchdog] still provides a slower
 * cooldown-based safety net for stuck sagas.
 */
open class SagaCompensationEngine<T : SagaStep<T>, TCommand : Any>(
    private val sagaStepRepository: SagaStepRepositoryPort<T>,
    private val commandDeserializer: CompensationCommandDeserializer<TCommand>,
    private val stepFactory: SagaCompensationStepFactory<T>,
    private val handler: CompensationCommandHandler<TCommand>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Entry point for inbound compensation events.
     *
     * Deserializes [payload] via [CompensationCommandDeserializer],
     * delegates the domain action to [CompensationCommandHandler], and
     * records a `Compensate<originalStep>` audit row via
     * [SagaCompensationStepFactory].
     *
     * Exceptions are intentionally propagated to the caller so the Kafka
     * listener can trigger broker-level retry / DLT routing. Idempotency
     * is preserved by the inbound `ProcessedEventGuard` (consumer side)
     * and by the unique-constraint on saga step `(sagaId, stepName)`.
     */
    @Transactional
    open fun handleCompensationEvent(payload: ByteArray) {
        val decoded = commandDeserializer.deserialize(payload)
        logger.info(
            "Processing compensation command: {} for saga {}",
            decoded.command::class.simpleName,
            decoded.sagaId,
        )

        handler.handle(decoded.sagaId, decoded.command)

        recordCompensationStep(decoded.sagaId, decoded.stepId)
    }

    private fun recordCompensationStep(
        sagaId: String,
        stepId: Long?,
    ) {
        val id = stepId ?: return
        val step =
            sagaStepRepository.findOneById(id)
                ?: error("Saga step '$id' of saga '$sagaId' not found")

        val compensationStep =
            stepFactory.createCompensationStep(
                CompensationStepDraft(
                    sagaId = sagaId,
                    stepName = "$COMPENSATION_PREFIX${step.stepName}",
                    status = SagaStepStatus.COMPENSATED,
                    createdAt = Instant.now(),
                    completedAt = Instant.now(),
                    compensationStepId = id,
                ),
            )
        sagaStepRepository.save(compensationStep)
    }

    /** Names shared with the code that reads compensation steps. */
    companion object {
        /** What a compensation step puts before the name of the step it undoes. */
        const val COMPENSATION_PREFIX = "Compensate"
    }
}
