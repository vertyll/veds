package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.Saga
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaRepositoryPort
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStepRepositoryPort
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Reusable saga participant engine for the **choreography** pattern.
 *
 * The engine is built on top of persistence-agnostic ports
 * ([SagaRepositoryPort], [SagaStepRepositoryPort]) and collaborator hooks
 * ([SagaEntityFactory], [SagaCompensator]). The underlying storage can be
 * JPA, MongoDB, DynamoDB, in-memory, etc. — the engine has no knowledge of it.
 *
 * State transitions are performed exclusively through the immutable
 * behavior methods exposed by [Saga] and [SagaStep] (DDD rich aggregate).
 * The engine never mutates fields directly.
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
open class SagaEngine<S : Saga<S>, T : SagaStep<T>>(
    private val sagaRepository: SagaRepositoryPort<S>,
    private val sagaStepRepository: SagaStepRepositoryPort<T>,
    private val objectMapper: ObjectMapper,
    private val entityFactory: SagaEntityFactory<S, T>,
    /**
     * Separate Spring bean that runs the compensation sequence in its own
     * `REQUIRES_NEW` transaction. Extracted out of [SagaEngine] so the call
     * goes through Spring's AOP proxy (self-invocation would bypass it).
     */
    private val compensationRunner: SagaCompensationRunner<S, T>,
) {
    private val logger: Logger = LoggerFactory.getLogger(SagaEngine::class.java)

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
            val transitioned = applyStepStatus(existingStep, status, defaultErrorReason = "Step '$stepName' failed")
            val updatedStep = sagaStepRepository.save(transitioned)
            handleStepFailure(sagaId, stepName, status)
            return updatedStep
        }

        val saga =
            sagaRepository.findOneById(sagaId)
                ?: throw IllegalArgumentException("Saga with id '$sagaId' not found")

        if (saga.status.isTerminal()) {
            logger.warn("Saga '$sagaId' is already in terminal status '${saga.status}' — ignoring new step '$stepName'")
            val skippedStep =
                entityFactory.createSagaStep(
                    sagaId = sagaId,
                    stepName = stepName,
                    status = SagaStepStatus.STARTED,
                    payload = payloadJson,
                    createdAt = Instant.now(),
                )
            val transitioned =
                applyStepStatus(
                    skippedStep,
                    status,
                    defaultErrorReason = "Step '$stepName' failed (saga already ${saga.status})",
                )
            return sagaStepRepository.save(transitioned)
        }

        val step =
            entityFactory.createSagaStep(
                sagaId = sagaId,
                stepName = stepName,
                status = SagaStepStatus.STARTED,
                payload = payloadJson,
                createdAt = Instant.now(),
            )
        val transitioned = applyStepStatus(step, status, defaultErrorReason = "Step '$stepName' failed")
        val savedStep = sagaStepRepository.save(transitioned)
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
            sagaRepository.findOneById(sagaId)
                ?: throw IllegalArgumentException("Saga with id '$sagaId' not found")

        if (saga.status.isTerminal()) return

        sagaRepository.save(saga.startCompensating("Step '$stepName' failed"))
        scheduleCompensationAfterCommit(sagaId)
    }

    // ── Explicit saga state transitions ─────────────────────────────────

    @Transactional
    open fun completeSaga(sagaId: String): S {
        val saga =
            sagaRepository.findOneById(sagaId)
                ?: throw IllegalArgumentException("Saga with id '$sagaId' not found")
        if (saga.status.isTerminal()) {
            logger.warn("Ignoring completeSaga for saga '$sagaId' — already in terminal status '${saga.status}'")
            return saga
        }
        logger.info("Saga '$sagaId' explicitly marked COMPLETED")
        return sagaRepository.save(saga.markCompleted())
    }

    open fun findSagaById(sagaId: String): S? = sagaRepository.findOneById(sagaId)

    @Transactional
    open fun awaitResponse(sagaId: String): S {
        val saga =
            sagaRepository.findOneById(sagaId)
                ?: throw IllegalArgumentException("Saga with id '$sagaId' not found")
        if (saga.status.isTerminal()) {
            logger.warn("Ignoring awaitResponse for saga '$sagaId' — already in terminal status '${saga.status}'")
            return saga
        }
        logger.info("Saga '$sagaId' marked AWAITING_RESPONSE")
        return sagaRepository.save(saga.markAwaitingResponse())
    }

    @Transactional
    open fun failSaga(
        sagaId: String,
        error: String,
    ): S {
        val saga =
            sagaRepository.findOneById(sagaId)
                ?: throw IllegalArgumentException("Saga with id '$sagaId' not found")
        if (saga.status.isTerminal()) {
            logger.warn("Ignoring failSaga for saga '$sagaId' — already in terminal status '${saga.status}'")
            return saga
        }
        val savedSaga = sagaRepository.save(saga.startCompensating(error))
        scheduleCompensationAfterCommit(sagaId)
        return savedSaga
    }

    // ── Compensation ────────────────────────────────────────────────────

    /**
     * Registers an after-commit hook that delegates to the proxied
     * [SagaCompensationRunner.runCompensation] (which opens a fresh
     * `REQUIRES_NEW` transaction). Decouples broker IO and step-compensation
     * work from the caller's business transaction; if the caller's
     * transaction rolls back, the hook is never executed.
     */
    private fun scheduleCompensationAfterCommit(sagaId: String) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        compensationRunner.runCompensation(sagaId)
                    }
                },
            )
        } else {
            // No active transaction (defensive) — run immediately through the
            // proxy so a real transaction is opened.
            compensationRunner.runCompensation(sagaId)
        }
    }

    /**
     * Public entry point used by [SagaWatchdog] to retry compensation for
     * sagas stuck in COMPENSATING or that ended in COMPENSATION_FAILED.
     * Idempotent — safe to call multiple times.
     */
    open fun runCompensation(sagaId: String) {
        compensationRunner.runCompensation(sagaId)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Applies a status transition to a freshly-created step (one whose
     * canonical status is [SagaStepStatus.STARTED]). Uses the port's
     * behavior methods so the engine never touches the entity's fields.
     */
    private fun applyStepStatus(
        step: T,
        target: SagaStepStatus,
        defaultErrorReason: String,
    ): T =
        when (target) {
            SagaStepStatus.STARTED, SagaStepStatus.PARTIALLY_COMPLETED -> step
            SagaStepStatus.COMPLETED -> step.markCompleted()
            SagaStepStatus.FAILED -> step.markFailed(step.errorMessage ?: defaultErrorReason)
            SagaStepStatus.COMPENSATED -> step.markCompensated()
            SagaStepStatus.COMPENSATION_FAILED -> step.markCompensationFailed(step.errorMessage ?: defaultErrorReason)
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
