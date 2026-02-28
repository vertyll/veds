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
 * Base saga manager that provides common saga coordination logic.
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

    protected lateinit var self: BaseSagaManager<S, T>

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        self = applicationContext.getBean(this::class.java)
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
     * Idempotent: if a step with the same name already exists, it is returned unchanged.
     *
     * When [status] is [SagaStepStatus.FAILED] compensation is triggered automatically.
     * When [status] is [SagaStepStatus.COMPLETED] the saga is marked COMPLETED if all
     * expected steps have been recorded as completed.
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
        val existingSteps = sagaStepRepository.findBySagaIdAndStepName(sagaId, stepName)
        if (existingSteps.isNotEmpty()) {
            val existingStep = existingSteps.first()
            logger.info("Saga step '$stepName' already exists for saga '$sagaId' (status: ${existingStep.status}) — skipping")
            return existingStep
        }

        val payloadJson = payload?.let { it as? String ?: objectMapper.writeValueAsString(it) }

        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
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
                }
                sagaRepository.save(saga)
            }
            else -> Unit
        }

        return savedStep
    }

    /**
     * Explicitly marks a saga as COMPLETED regardless of step state.
     */
    @Transactional
    open fun completeSaga(sagaId: String): S {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with id '$sagaId' not found")
            }

        saga.status = SagaStatus.COMPLETED
        saga.completedAt = Instant.now()

        return sagaRepository.save(saga)
    }

    /**
     * Marks a saga as FAILED and triggers compensation for all completed steps.
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

        saga.status = SagaStatus.FAILED
        saga.lastError = error
        saga.updatedAt = Instant.now()

        val savedSaga = sagaRepository.save(saga)
        triggerCompensation(savedSaga)

        return savedSaga
    }

    protected open fun areAllStepsCompleted(saga: S): Boolean {
        val expectedSteps = getSagaStepDefinitions()[saga.type] ?: return false

        val completedStepNames =
            sagaStepRepository
                .findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPLETED)
                .map { it.stepName }
                .toSet()

        return expectedSteps.all { it in completedStepNames }
    }

    protected open fun triggerCompensation(saga: S) {
        val completedSteps =
            sagaStepRepository
                .findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPLETED)
                .sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga '${saga.id}' — ${completedSteps.size} step(s) to compensate")

        completedSteps.forEach { step ->
            try {
                logger.info("Compensating step '${step.stepName}' (id: ${step.id}) for saga '${saga.id}'")
                compensateStep(saga, step)
                step.status = SagaStepStatus.COMPENSATED
                sagaStepRepository.save(step)
            } catch (e: Exception) {
                logger.error("Failed to compensate step '${step.stepName}' for saga '${saga.id}': ${e.message}", e)
                step.errorMessage = e.message
                sagaStepRepository.save(step)
            }
        }

        saga.status = SagaStatus.COMPENSATED
        saga.updatedAt = Instant.now()
        sagaRepository.save(saga)
    }
}
