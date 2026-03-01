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
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Base saga **orchestrator** that provides common saga coordination logic.
 *
 * This class follows the **Saga Orchestrator** pattern: it knows the full list
 * of expected steps ([getSagaStepDefinitions]), tracks overall saga state, and
 * decides when to trigger compensation. Communication with other services is
 * event-driven (Kafka), but the coordination logic is centralized here.
 *
 * Services should extend this class and implement service-specific methods.
 *
 * Implements [ApplicationContextAware] to get a reference to the Spring
 * proxy of the concrete subclass. This allows enum-typed overloads to delegate
 * through the proxy, ensuring @Transactional is always honored — even when
 * called from within the same bean.
 *
 * No @Autowired, no late init var for injection, no circular dependency issues.
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
     * Returns saga step definitions for each saga type.
     * Map key is the saga type value, value is a list of expected step name values.
     */
    protected abstract fun getSagaStepDefinitions(): Map<String, List<String>>

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
     * Records a step in an existing saga.
     *
     * Idempotent for the same status: if a step with the same name already exists
     * and has the same status, it is returned unchanged.
     *
     * Allows status transitions: if the existing step has a non-terminal status
     * (e.g., STARTED) and a new terminal status is provided (e.g., COMPLETED, FAILED),
     * the existing step is updated rather than duplicated.
     *
     * When [status] is [SagaStepStatus.FAILED] compensation is triggered automatically.
     * When [status] is [SagaStepStatus.COMPLETED] the saga is marked COMPLETED if all
     * expected steps have been recorded as completed, or AWAITING_RESPONSE if some
     * asynchronous steps are still pending.
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
            updateSagaStatus(sagaId, stepName, status)
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

        var savedStep = sagaStepRepository.save(step)

        if (status == SagaStepStatus.COMPLETED) {
            savedStep.completedAt = Instant.now()
            savedStep = sagaStepRepository.save(savedStep)
        }

        updateSagaStatus(sagaId, stepName, status)

        return savedStep
    }

    private fun updateSagaStatus(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
    ) {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }

        if (saga.status.isTerminal()) return

        when (status) {
            SagaStepStatus.FAILED -> {
                saga.status = SagaStatus.COMPENSATING
                saga.lastError = "Step '$stepName' failed"
                saga.updatedAt = Instant.now()
                sagaRepository.save(saga)
                triggerCompensation(saga)
            }
            SagaStepStatus.PARTIALLY_COMPLETED -> {
                saga.status = SagaStatus.PARTIALLY_COMPLETED
                saga.updatedAt = Instant.now()
                saga.completedAt = Instant.now()
                logger.info("Saga '${saga.id}' partially completed")
                sagaRepository.save(saga)
            }
            SagaStepStatus.COMPLETED -> {
                saga.updatedAt = Instant.now()
                if (areAllStepsCompleted(saga)) {
                    saga.status = SagaStatus.COMPLETED
                    saga.completedAt = Instant.now()
                    logger.info("All steps completed for saga '${saga.id}' — marking COMPLETED")
                } else if (hasAsyncStepsPending(saga)) {
                    saga.status = SagaStatus.AWAITING_RESPONSE
                    logger.info("Saga '${saga.id}' is awaiting response from external service(s)")
                }
                sagaRepository.save(saga)
            }
            else -> Unit
        }
    }

    /**
     * Marks a saga as FAILED and triggers compensation for all completed steps.
     *
     * Guard clause: if the saga is already in a terminal state (COMPLETED, COMPENSATED,
     * COMPENSATION_FAILED), the call is ignored to prevent out-of-order event issues
     * in choreography-based communication.
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

        saga.status = SagaStatus.FAILED
        saga.lastError = error
        saga.updatedAt = Instant.now()

        val savedSaga = sagaRepository.save(saga)
        triggerCompensation(savedSaga)

        return savedSaga
    }

    /**
     * Checks if all expected steps for this saga type have been completed.
     */
    protected open fun areAllStepsCompleted(saga: S): Boolean {
        val expectedSteps = getSagaStepDefinitions()[saga.type] ?: return false

        val completedStepNames =
            sagaStepRepository
                .findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPLETED)
                .map { it.stepName }
                .toSet()

        return expectedSteps.all { it in completedStepNames }
    }

    /**
     * Checks if the saga has completed some steps but is still waiting for
     * asynchronous steps from external services (e.g., mail delivery confirmation).
     */
    protected open fun hasAsyncStepsPending(saga: S): Boolean {
        val expectedSteps = getSagaStepDefinitions()[saga.type] ?: return false

        val recordedStepNames =
            sagaStepRepository
                .findBySagaId(saga.id)
                .map { it.stepName }
                .toSet()

        val missingSteps = expectedSteps.filter { it !in recordedStepNames }
        return missingSteps.isNotEmpty()
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
