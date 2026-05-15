package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaRepository
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Reusable saga participant engine for the **choreography** pattern.
 *
 * This class replaces the legacy `BaseSagaManager` Template Method base class
 * with **composition**: it has no abstract methods and is not designed for
 * inheritance. Service-specific behavior is injected via two collaborator
 * interfaces:
 *
 *  - [SagaEntityFactory] — creates concrete `*JpaEntity` instances
 *  - [SagaCompensator]   — performs domain-specific compensation per step
 *
 * The engine itself stays a `final` Spring bean per microservice, so
 * `@Transactional` is honored naturally without any `ApplicationContextAware`
 * proxy-self tricks.
 *
 * Choreography semantics (no central orchestrator):
 *  - [startSaga] — begins a new local saga
 *  - [recordSagaStep] — records a local step (does NOT auto-complete the saga)
 *  - [awaitResponse] — marks the saga as waiting for an external event
 *  - [completeSaga] — explicitly marks the saga as completed
 *  - [failSaga] — explicitly marks the saga as failed and triggers compensation
 *
 * When a step is recorded with [SagaStepStatus.FAILED], compensation is still
 * triggered automatically because a local step failure is an immediate signal.
 */
open class SagaEngine<S : BaseSaga, T : BaseSagaStep>(
    private val sagaRepository: BaseSagaRepository<S>,
    private val sagaStepRepository: BaseSagaStepRepository<T>,
    private val kafkaOutboxProcessor: KafkaOutboxProcessor,
    private val objectMapper: ObjectMapper,
    private val entityFactory: SagaEntityFactory<S, T>,
    private val compensator: SagaCompensator<S, T>,
    /**
     * Kafka topic to which compensation events for the owning service are
     * published (e.g. `saga-compensation-iam`).
     */
    private val compensationTopic: String,
) {
    private val logger: Logger = LoggerFactory.getLogger(SagaEngine::class.java)

    private val compensationContext: SagaCompensationContext =
        object : SagaCompensationContext {
            override fun publishCompensationEvent(
                sagaId: String,
                stepId: Long?,
                action: String,
                extraPayload: Map<String, Any?>,
            ) {
                publishCompensation(sagaId, stepId, action, extraPayload)
            }

            override fun readStepPayload(payload: String?): Map<String, Any?> {
                if (payload.isNullOrBlank()) return emptyMap()
                @Suppress("UNCHECKED_CAST")
                return objectMapper.readValue(payload, Map::class.java) as Map<String, Any?>
            }
        }

    // ── Enum-typed convenience overloads ────────────────────────────────

    @Transactional
    open fun startSaga(
        sagaType: SagaTypeValue,
        payload: Any,
    ): S = startSaga(sagaType.value, payload)

    @Transactional
    open fun recordSagaStep(
        sagaId: String,
        stepName: SagaTypeValue,
        status: SagaStepStatus,
        payload: Any? = null,
    ): T = recordSagaStep(sagaId, stepName.value, status, payload)

    // ── Core saga operations ────────────────────────────────────────────

    @Transactional
    open fun startSaga(
        sagaType: String,
        payload: Any,
    ): S {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)
        val saga =
            entityFactory.createSaga(
                id = UUID.randomUUID().toString(),
                type = sagaType,
                status = SagaStatus.STARTED,
                payload = payloadJson,
                startedAt = Instant.now(),
            )
        logger.info("Starting saga of type '$sagaType' with id '${saga.id}'")
        return sagaRepository.save(saga)
    }

    @Transactional
    open fun recordSagaStep(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: Any? = null,
    ): T {
        val payloadJson = payload?.let { it as? String ?: objectMapper.writeValueAsString(it) }

        val existingSteps = sagaStepRepository.findBySagaIdAndStepName(sagaId, stepName)
        if (existingSteps.isNotEmpty()) {
            val existingStep = existingSteps.first()
            if (existingStep.status == status) {
                logger.info("Saga step '$stepName' already exists for saga '$sagaId' with same status ($status) — skipping")
                return existingStep
            }
            if (existingStep.status in TERMINAL_STEP_STATUSES) {
                logger.info(
                    "Saga step '$stepName' for saga '$sagaId' is already terminal (${existingStep.status}) — skipping update to $status",
                )
                return existingStep
            }
            logger.info("Updating saga step '$stepName' for saga '$sagaId': ${existingStep.status} → $status")
            existingStep.status = status
            if (status == SagaStepStatus.COMPLETED) {
                existingStep.completedAt = Instant.now()
            }
            if (status == SagaStepStatus.FAILED) {
                existingStep.errorMessage = "Step '$stepName' failed"
            }
            val updatedStep = sagaStepRepository.save(existingStep)
            handleStepFailure(sagaId, stepName, status)
            return updatedStep
        }

        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }

        if (saga.status.isTerminal()) {
            logger.warn("Saga '$sagaId' is already in terminal status '${saga.status}' — ignoring new step '$stepName'")
            val skippedStep =
                entityFactory.createSagaStep(
                    sagaId = sagaId,
                    stepName = stepName,
                    status = status,
                    payload = payloadJson,
                    createdAt = Instant.now(),
                )
            if (status == SagaStepStatus.FAILED) {
                skippedStep.errorMessage = "Step '$stepName' failed (saga already ${saga.status})"
            }
            return sagaStepRepository.save(skippedStep)
        }

        val step =
            entityFactory.createSagaStep(
                sagaId = sagaId,
                stepName = stepName,
                status = status,
                payload = payloadJson,
                createdAt = Instant.now(),
            )

        if (status == SagaStepStatus.COMPLETED) {
            step.completedAt = Instant.now()
        }

        val savedStep = sagaStepRepository.save(step)
        handleStepFailure(sagaId, stepName, status)
        return savedStep
    }

    private fun handleStepFailure(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ) {
        if (status != SagaStepStatus.FAILED) return

        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }

        if (saga.status.isTerminal()) return

        saga.status = SagaStatus.COMPENSATING
        saga.lastError = "Step '$stepName' failed"
        saga.updatedAt = Instant.now()
        sagaRepository.save(saga)
        triggerCompensation(saga)
    }

    // ── Explicit saga state transitions ─────────────────────────────────

    @Transactional
    open fun completeSaga(sagaId: String): S {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }
        if (saga.status.isTerminal()) {
            logger.warn("Ignoring completeSaga for saga '$sagaId' — already in terminal status '${saga.status}'")
            return saga
        }
        saga.status = SagaStatus.COMPLETED
        saga.completedAt = Instant.now()
        saga.updatedAt = Instant.now()
        logger.info("Saga '$sagaId' explicitly marked COMPLETED")
        return sagaRepository.save(saga)
    }

    open fun findSagaById(sagaId: String): S? = sagaRepository.findById(sagaId).orElse(null)

    @Transactional
    open fun awaitResponse(sagaId: String): S {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }
        if (saga.status.isTerminal()) {
            logger.warn("Ignoring awaitResponse for saga '$sagaId' — already in terminal status '${saga.status}'")
            return saga
        }
        saga.status = SagaStatus.AWAITING_RESPONSE
        saga.updatedAt = Instant.now()
        logger.info("Saga '$sagaId' marked AWAITING_RESPONSE")
        return sagaRepository.save(saga)
    }

    @Transactional
    open fun failSaga(
        sagaId: String,
        error: String,
    ): S {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }
        if (saga.status.isTerminal()) {
            logger.warn("Ignoring failSaga for saga '$sagaId' — already in terminal status '${saga.status}'")
            return saga
        }
        saga.status = SagaStatus.COMPENSATING
        saga.lastError = error
        saga.updatedAt = Instant.now()
        val savedSaga = sagaRepository.save(saga)
        triggerCompensation(savedSaga)
        return savedSaga
    }

    // ── Compensation ────────────────────────────────────────────────────

    private fun publishCompensation(
        sagaId: String,
        stepId: Long?,
        action: String,
        extraPayload: Map<String, Any?>,
    ) {
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = compensationTopic,
            key = sagaId,
            payload =
                buildMap {
                    put("sagaId", sagaId)
                    put("stepId", stepId)
                    put("action", action)
                    putAll(extraPayload)
                },
            sagaId = sagaId,
        )
    }

    private fun triggerCompensation(saga: S) {
        val completedSteps =
            sagaStepRepository
                .findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPLETED)
                .sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga '${saga.id}' — ${completedSteps.size} step(s) to compensate")

        var allCompensated = true

        completedSteps.forEach { step ->
            try {
                logger.info("Compensating step '${step.stepName}' (id: ${step.id}) for saga '${saga.id}'")
                compensator.compensateStep(saga, step, compensationContext)
                step.status = SagaStepStatus.COMPENSATED
                sagaStepRepository.save(step)
            } catch (e: Exception) {
                logger.error("Failed to compensate step '${step.stepName}' for saga '${saga.id}': ${e.message}", e)
                step.status = SagaStepStatus.COMPENSATION_FAILED
                step.errorMessage = e.message
                sagaStepRepository.save(step)
                allCompensated = false
            }
        }

        if (allCompensated) {
            saga.status = SagaStatus.COMPENSATED
            logger.info("All steps compensated for saga '${saga.id}' — marking COMPENSATED")
        } else {
            saga.status = SagaStatus.COMPENSATION_FAILED
            logger.error("Some steps failed compensation for saga '${saga.id}' — marking COMPENSATION_FAILED")
        }
        saga.updatedAt = Instant.now()
        sagaRepository.save(saga)
    }

    private companion object {
        val TERMINAL_STEP_STATUSES =
            setOf(
                SagaStepStatus.COMPLETED,
                SagaStepStatus.FAILED,
                SagaStepStatus.COMPENSATED,
                SagaStepStatus.COMPENSATION_FAILED,
            )
    }
}
