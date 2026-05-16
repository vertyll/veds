package com.vertyll.veds.sharedinfrastructure.saga.service

import com.vertyll.veds.sharedinfrastructure.saga.contract.Saga
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaRepositoryPort
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStep
import com.vertyll.veds.sharedinfrastructure.saga.contract.SagaStepRepositoryPort
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStepStatus
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Transactional helper for [SagaEngine] that runs the compensation
 * sequence in its own fresh transaction.
 *
 * Extracted into a separate Spring bean so that calls go through the AOP
 * proxy and actually open the requested `REQUIRES_NEW` transaction —
 * `@Transactional` self-invocation on the same instance bypasses the proxy
 * and runs without a transaction.
 *
 * Created per service (one bean per `SagaEngine`) to keep the type
 * parameters consistent with the owning engine.
 */
open class SagaCompensationRunner<S : Saga<S>, T : SagaStep<T>>(
    private val sagaRepository: SagaRepositoryPort<S>,
    private val sagaStepRepository: SagaStepRepositoryPort<T>,
    private val compensator: SagaCompensator<S, T>,
    private val compensationContext: SagaCompensationContext,
) {
    private val logger = LoggerFactory.getLogger(SagaCompensationRunner::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun runCompensation(sagaId: String) {
        val saga = sagaRepository.findOneById(sagaId) ?: return
        if (saga.status == SagaStatus.COMPLETED ||
            saga.status == SagaStatus.COMPENSATED ||
            saga.status == SagaStatus.FAILED
        ) {
            return
        }
        triggerCompensation(saga)
    }

    private fun triggerCompensation(saga: S) {
        val pendingSteps =
            (
                sagaStepRepository.findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPLETED) +
                    sagaStepRepository.findBySagaIdAndStatus(saga.id, SagaStepStatus.COMPENSATION_FAILED)
            ).sortedByDescending { it.createdAt }

        if (pendingSteps.isEmpty()) {
            logger.info("No steps left to compensate for saga '${saga.id}' — marking COMPENSATED")
            sagaRepository.save(saga.markCompensated())
            return
        }

        logger.info("Triggering compensation for saga '${saga.id}' — ${pendingSteps.size} step(s) to compensate")

        var allCompensated = true
        pendingSteps.forEach { step ->
            try {
                logger.info("Compensating step '${step.stepName}' (id: ${step.id}) for saga '${saga.id}'")
                compensator.compensateStep(saga, step, compensationContext)
                sagaStepRepository.save(step.markCompensated())
            } catch (e: Exception) {
                logger.error("Failed to compensate step '${step.stepName}' for saga '${saga.id}': ${e.message}", e)
                sagaStepRepository.save(step.markCompensationFailed(e.message))
                allCompensated = false
            }
        }

        if (allCompensated) {
            logger.info("All steps compensated for saga '${saga.id}' — marking COMPENSATED")
            sagaRepository.save(saga.markCompensated())
        } else {
            logger.error(
                "Some steps failed compensation for saga '${saga.id}' — marking COMPENSATION_FAILED " +
                    "(watchdog will retry)",
            )
            sagaRepository.save(saga.markCompensationFailed())
        }
    }
}
