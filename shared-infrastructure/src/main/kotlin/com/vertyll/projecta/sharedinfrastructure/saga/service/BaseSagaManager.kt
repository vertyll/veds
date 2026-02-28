package com.vertyll.projecta.sharedinfrastructure.saga.service

import com.vertyll.projecta.sharedinfrastructure.kafka.KafkaOutboxProcessor
import com.vertyll.projecta.sharedinfrastructure.saga.entity.BaseSaga
import com.vertyll.projecta.sharedinfrastructure.saga.entity.BaseSagaStep
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.projecta.sharedinfrastructure.saga.enums.SagaStepStatus
import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaRepository
import com.vertyll.projecta.sharedinfrastructure.saga.repository.BaseSagaStepRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Base saga manager that provides common saga coordination logic.
 * Services should extend this class and implement service-specific methods.
 *
 * @param S The saga entity type that extends BaseSaga
 * @param T The saga step entity type that extends BaseSagaStep
 */
abstract class BaseSagaManager<S : BaseSaga, T : BaseSagaStep>(
    protected val sagaRepository: BaseSagaRepository<S>,
    protected val sagaStepRepository: BaseSagaStepRepository<T>,
    protected val kafkaOutboxProcessor: KafkaOutboxProcessor,
    protected val objectMapper: ObjectMapper,
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Returns saga step definitions for each saga type.
     * Map key is the saga type, value is list of expected step names.
     * Example: mapOf("USER_REGISTRATION" to listOf("CREATE_USER", "SEND_EMAIL"))
     */
    protected abstract fun getSagaStepDefinitions(): Map<String, List<String>>

    /**
     * Creates a new saga instance. Services must implement this to provide
     * their specific saga entity type.
     */
    protected abstract fun createSagaEntity(
        id: String,
        type: String,
        status: SagaStatus,
        payload: String,
        startedAt: Instant,
    ): S

    /**
     * Creates a new saga step instance. Services must implement this to provide
     * their specific saga step entity type.
     */
    protected abstract fun createSagaStepEntity(
        sagaId: String,
        stepName: String,
        status: SagaStepStatus,
        payload: String?,
        createdAt: Instant,
    ): T

    /**
     * Compensates a specific step. Services must implement compensation logic
     * for their specific step types.
     */
    protected abstract fun compensateStep(
        saga: S,
        step: T,
    )

    /**
     * Starts a new saga
     * @param sagaType The type of saga to start
     * @param payload Additional data related to the saga
     * @return The created saga instance
     */
    @Transactional
    open fun startSaga(
        sagaType: String,
        payload: Any,
    ): S {
        val payloadJson = payload as? String ?: objectMapper.writeValueAsString(payload)

        val saga =
            createSagaEntity(
                id =
                    java.util.UUID
                        .randomUUID()
                        .toString(),
                type = sagaType,
                status = SagaStatus.STARTED,
                payload = payloadJson,
                startedAt = Instant.now(),
            )

        return sagaRepository.save(saga)
    }

    /**
     * Records a step in a saga
     * @param sagaId The ID of the saga
     * @param stepName The name of the step
     * @param status The status of the step
     * @param payload Additional data related to the step
     * @return The created saga step
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
            logger.info("Saga step $stepName already exists for saga $sagaId, status: ${existingStep.status}")
            return existingStep
        }

        val payloadJson =
            payload?.let {
                it as? String ?: objectMapper.writeValueAsString(it)
            }

        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        val step =
            createSagaStepEntity(
                sagaId = sagaId,
                stepName = stepName,
                status = status,
                payload = payloadJson,
                createdAt = Instant.now(),
            )

        val savedStep = sagaStepRepository.save(step)

        if (status == SagaStepStatus.COMPLETED) {
            savedStep.completedAt = Instant.now()
            sagaStepRepository.save(savedStep)
        }

        if (status == SagaStepStatus.FAILED) {
            saga.status = SagaStatus.COMPENSATING
            saga.lastError = "Step $stepName failed"
            sagaRepository.save(saga)

            triggerCompensation(saga)
        } else if (status == SagaStepStatus.COMPLETED) {
            saga.updatedAt = Instant.now()

            if (areAllStepsCompleted(saga)) {
                saga.status = SagaStatus.COMPLETED
                saga.completedAt = Instant.now()
                logger.info("All steps completed for saga ${saga.id}, marking as COMPLETED")
            }

            sagaRepository.save(saga)
        }

        return savedStep
    }

    /**
     * Checks if all expected steps for a saga have been completed
     * @param saga The saga to check
     * @return True if all expected steps are completed, false otherwise
     */
    protected open fun areAllStepsCompleted(saga: S): Boolean {
        val expectedSteps = getSagaStepDefinitions()[saga.type] ?: return false

        val completedSteps =
            sagaStepRepository.findBySagaIdAndStatus(
                saga.id,
                SagaStepStatus.COMPLETED,
            )

        val completedStepNames = completedSteps.map { it.stepName }

        return expectedSteps.all { expectedStep ->
            completedStepNames.contains(expectedStep)
        }
    }

    /**
     * Marks a saga as completed
     * @param sagaId The ID of the saga to complete
     * @return The updated saga
     */
    @Transactional
    open fun completeSaga(sagaId: String): S {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        saga.status = SagaStatus.COMPLETED
        saga.completedAt = Instant.now()

        return sagaRepository.save(saga)
    }

    /**
     * Marks a saga as failed and initiates compensation
     * @param sagaId The ID of the saga that failed
     * @param error The error that caused the failure
     * @return The updated saga
     */
    @Transactional
    open fun failSaga(
        sagaId: String,
        error: String,
    ): S {
        val saga =
            sagaRepository.findById(sagaId).orElseThrow {
                IllegalArgumentException("Saga with ID $sagaId not found")
            }

        saga.status = SagaStatus.FAILED
        saga.lastError = error
        saga.updatedAt = Instant.now()

        val savedSaga = sagaRepository.save(saga)

        triggerCompensation(savedSaga)

        return savedSaga
    }

    /**
     * Triggers compensation for a failed saga
     * @param saga The saga to compensate
     */
    protected open fun triggerCompensation(saga: S) {
        val completedSteps =
            sagaStepRepository
                .findBySagaIdAndStatus(
                    saga.id,
                    SagaStepStatus.COMPLETED,
                ).sortedByDescending { it.createdAt }

        logger.info("Triggering compensation for saga ${saga.id} with ${completedSteps.size} steps to compensate")

        completedSteps.forEach { step ->
            try {
                logger.info("Compensating step ${step.stepName} (ID: ${step.id}) for saga ${saga.id}")
                compensateStep(saga, step)

                step.status = SagaStepStatus.COMPENSATED
                sagaStepRepository.save(step)
            } catch (e: Exception) {
                logger.error("Failed to compensate step ${step.stepName} for saga ${saga.id}: ${e.message}", e)
                step.errorMessage = e.message
                sagaStepRepository.save(step)
            }
        }

        saga.status = SagaStatus.COMPENSATED
        saga.updatedAt = Instant.now()
        sagaRepository.save(saga)
    }
}
