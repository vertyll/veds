package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.event.EventSource
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.veds.sharedinfrastructure.kafka.KafkaTopicNames
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaTypeValue
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.veds.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaRepository
import com.vertyll.veds.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Base saga participant for the **choreography** pattern.
 *
 * In choreography no single component knows the full cross-service flow.
 * Each service tracks only **its own local steps** and reacts to domain
 * events published by other services. The calling code drives saga state transitions
 * explicitly:
 *
 * - [startSaga] — begins a new local saga
 * - [recordSagaStep] — records a local step (does NOT auto-complete the saga)
 * - [awaitResponse] — marks the saga as waiting for an external event
 * - [completeSaga] — explicitly marks the saga as completed (called when an
 *   incoming event confirms the full flow succeeded)
 * - [failSaga] — explicitly marks the saga as failed and triggers local compensation
 *
 * When a step is recorded with [SagaStepStatus.FAILED], compensation is still
 * triggered automatically because a local step failure is an immediate signal.
 *
 * Services should extend this class and implement the abstract factory and
 * compensation methods.
 *
 * Implements [ApplicationContextAware] to get a reference to the Spring
 * proxy of the concrete subclass. This allows enum-typed overloads to delegate
 * through the proxy, ensuring @Transactional is always honored — even when
 * called from within the same bean.
 *
 * @param S The saga entity type that extends BaseSaga
 * @param T The saga step entity type that extends BaseSagaStep
 */
abstract class BaseSagaManager<S : BaseSaga, T : BaseSagaStep>(
    protected val sagaRepository: BaseSagaRepository<S>,
    protected val sagaStepRepository: BaseSagaStepRepository<T>,
    protected val kafkaOutboxProcessor: KafkaOutboxProcessor,
    protected val objectMapper: ObjectMapper,
) : ApplicationContextAware {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    private lateinit var applicationContext: ApplicationContext

    protected val self: BaseSagaManager<S, T> by lazy {
        applicationContext.getBean(this::class.java)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Identifies this service for source filtering in compensation events.
     * Compensation messages include this value so that [BaseSagaCompensationService]
     * can filter out events intended for other services.
     */
    protected abstract val serviceSource: EventSource

    /**
     * Creates a new saga instance of the concrete type [S].
     */
    protected abstract fun createSagaEntity(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): S

    /**
     * Creates a new saga step instance of the concrete type [T].
     */
    protected abstract fun createSagaStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): T

    /**
     * Performs domain-specific compensation for a single step.
     * Called by [triggerCompensation] in reverse-chronological order.
     */
    protected abstract fun compensateStep(
        saga: S,
        step: T,
    )

    // ── Enum-typed convenience overloads ────────────────────────────────

    /**
     * Enum-typed entry point for starting a saga.
     * Delegates through the Spring proxy to the String overload.
     */
    @Transactional
    open fun startSaga(
        sagaType: SagaTypeValue,
        payload: Any,
    ): S = self.startSaga(sagaType.value, payload)

    /**
     * Enum-typed entry point for recording a saga step.
     * Delegates through the Spring proxy to the String overload.
     */
    @Transactional
    open fun recordSagaStep(
        sagaId: String,
        stepName: SagaTypeValue,
        status: SagaStepStatus,
        payload: Any? = null,
    ): T = self.recordSagaStep(sagaId, stepName.value, status, payload)

    // ── Core saga operations ────────────────────────────────────────────

    /**
     * Starts a new saga.
     *
     * @param sagaType Raw saga type string (e.g. "UserRegistration")
     * @param payload  Any serializable payload; Strings are stored as-is
     * @return The persisted saga instance
     */
    @Transactional
    open fun startSaga(
        sagaType: String,
        payload: Any,
    ): S {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val saga =
            createSagaEntity(
                id = UUID.randomUUID().toString(),
                type = sagaType,
                status = SagaStatus.STARTED,
                payload = payloadJson,
                startedAt = Instant.now(),
            )

        logger.info("Starting saga of type '$sagaType' with id '${saga.id}'")
        return sagaRepository.save(saga)
    }

    /**
     * Records a local step in an existing saga.
     *
     * **Choreography note:** recording a step does NOT automatically complete the
     * saga. Use [completeSaga] or [awaitResponse] to transition the saga state
     * explicitly based on domain events.
     *
     * Idempotent for the same status: if a step with the same name already exists
     * and has the same status, it is returned unchanged.
     *
     * Allows status transitions: if the existing step has a non-terminal status
     * (e.g., STARTED) and a new terminal status is provided (e.g., COMPLETED, FAILED),
     * the existing step is updated rather than duplicated.
     *
     * When [status] is [SagaStepStatus.FAILED] compensation is triggered automatically
     * because a local step failure is an immediate signal.
     *
     * @param sagaId   The saga to attach this step to
     * @param stepName Raw step name string (e.g. "CreateUser")
     * @param status   Outcome of this step
     * @param payload  Optional step payload; Strings are stored as-is
     * @return The persisted saga step
     */
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
                createSagaStepEntity(
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
            createSagaStepEntity(
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

    /**
     * If a local step failed, immediately trigger compensation.
     * Other statuses (COMPLETED, PARTIALLY_COMPLETED) do NOT change the saga
     * status — the calling code must use [completeSaga] or [awaitResponse].
     */
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

    // ── Explicit saga state transitions (choreography) ──────────────────

    /**
     * Explicitly marks a saga as COMPLETED.
     *
     * In choreography, this is called when an incoming event confirms
     * that the full cross-service flow has succeeded (e.g., a
     * [com.vertyll.veds.sharedinfrastructure.event.mail.MailSentEvent] confirms that the email was delivered).
     *
     * Guard clause: if the saga is already in a terminal state, the call
     * is ignored.
     */
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

    /**
     * Returns the saga entity for the given ID, or `null` if not found.
     */
    open fun findSagaById(sagaId: String): S? = sagaRepository.findById(sagaId).orElse(null)

    /**
     * Marks a saga as [SagaStatus.AWAITING_RESPONSE].
     *
     * Called after the local steps have been recorded, and the service is now
     * waiting for an event from another participant (e.g., mail-service).
     *
     * Guard clause: if the saga is already in a terminal state, the call
     * is ignored.
     */
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

    /**
     * Marks a saga as COMPENSATING and triggers compensation for all completed steps.
     *
     * The saga transitions: COMPENSATING → COMPENSATED (if all steps compensated)
     * or COMPENSATING → COMPENSATION_FAILED (if any step failed to compensate).
     *
     * Guard clause: if the saga is already in a terminal state (COMPLETED, FAILED,
     * COMPENSATED, COMPENSATION_FAILED), the call is ignored to prevent
     * out-of-order event issues.
     */
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
            logger.warn(
                "Ignoring failSaga for saga '$sagaId' — already in terminal status '${saga.status}'",
            )
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

    /**
     * Publishes a compensation event to the outbox for processing by
     * [BaseSagaCompensationService]. Automatically includes the [serviceSource]
     * so that only the originating service processes the event.
     *
     * @param sagaId       The saga being compensated
     * @param stepId       The original step ID being compensated
     * @param action       The compensation action identifier
     * @param extraPayload Additional action-specific data
     */
    protected fun publishCompensationEvent(
        sagaId: String,
        stepId: Long?,
        action: String,
        extraPayload: Map<String, Any?> = emptyMap(),
    ) {
        kafkaOutboxProcessor.saveOutboxMessage(
            topic = KafkaTopicNames.SAGA_COMPENSATION,
            key = sagaId,
            payload =
                buildMap {
                    put("sagaId", sagaId)
                    put("stepId", stepId)
                    put("action", action)
                    put("source", serviceSource.value)
                    putAll(extraPayload)
                },
            sagaId = sagaId,
        )
    }

    /**
     * Triggers compensation for all completed steps in reverse-chronological order.
     *
     * Tracks whether all compensations succeeded:
     * - If all succeed → saga status = COMPENSATED
     * - If any fail → saga status = COMPENSATION_FAILED (requires manual intervention)
     */
    protected open fun triggerCompensation(saga: S) {
        val completedSteps =
            sagaStepRepository
                .findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPLETED)
                .sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga '${saga.id}' — ${completedSteps.size} step(s) to compensate")

        var allCompensated = true

        completedSteps.forEach { step ->
            try {
                logger.info("Compensating step '${step.stepName}' (id: ${step.id}) for saga '${saga.id}'")
                compensateStep(saga, step)
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
        /**
         * Step statuses that should not be overwritten by a new recordSagaStep call.
         */
        val TERMINAL_STEP_STATUSES =
            setOf(
                SagaStepStatus.COMPLETED,
                SagaStepStatus.FAILED,
                SagaStepStatus.COMPENSATED,
                SagaStepStatus.COMPENSATION_FAILED,
            )
    }
}
