package com.vertyll.veds.shared.saga.engine

import com.vertyll.veds.shared.saga.Saga
import com.vertyll.veds.shared.saga.SagaStatus
import com.vertyll.veds.shared.saga.SagaStep
import com.vertyll.veds.shared.saga.engine.persistence.SagaRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant

/**
 * Per-service watchdog that:
 *  1. **Times out sagas** stuck in
 *     [SagaStatus.AWAITING_RESPONSE] for longer than
 *     [SagaProperties.awaitResponseTimeout] (calls
 *     [SagaEngine.failSaga] which schedules compensation).
 *  2. **Retries compensation** for sagas that are still
 *     [SagaStatus.COMPENSATING] or that ended in
 *     [SagaStatus.COMPENSATION_FAILED] and whose `updatedAt` is older than
 *     [SagaProperties.compensationRetryCooldown].
 *
 * Created per service (analogous to the per-service [SagaEngine] bean) so
 * each microservice's saga state machine has its own scheduled watchdog.
 */
open class SagaWatchdog<S : Saga<S>, T : SagaStep<T>>(
    private val sagaRepository: SagaRepositoryPort<S>,
    private val sagaEngine: SagaEngine<S, T>,
    private val properties: SagaProperties,
) {
    private val logger = LoggerFactory.getLogger(SagaWatchdog::class.java)

    /**
     * Scheduled tick fired every
     * `application.saga.watchdog-interval` (default `PT1M`). Times out stuck
     * `AWAITING_RESPONSE` sagas and retries failed compensations. Designed
     * to be idempotent and safe to run on every node — each operation goes
     * through [SagaEngine] which guards against re-entry on terminal sagas.
     */
    @Scheduled(fixedDelayString = $$"${application.saga.watchdog-interval:PT1M}")
    fun tick() {
        timeoutAwaitingResponseSagas()
        retryStuckCompensations()
    }

    private fun timeoutAwaitingResponseSagas() {
        val cutoff = Instant.now().minus(properties.awaitResponseTimeout)
        val timedOut =
            sagaRepository.findByStatusInAndUpdatedAtBefore(
                statuses = listOf(SagaStatus.AWAITING_RESPONSE),
                updatedAt = cutoff,
            )
        if (timedOut.isEmpty()) return

        logger.warn("SagaWatchdog: {} saga(s) timed out in AWAITING_RESPONSE — failing", timedOut.size)
        timedOut.forEach { saga ->
            runCatching {
                sagaEngine.failSaga(saga.id, "timeout")
            }.onFailure { e ->
                logger.error("SagaWatchdog: failed to time out saga '${saga.id}': ${e.message}", e)
            }
        }
    }

    private fun retryStuckCompensations() {
        val cutoff = Instant.now().minus(properties.compensationRetryCooldown)
        val candidates =
            sagaRepository.findByStatusInAndUpdatedAtBefore(
                statuses = listOf(SagaStatus.COMPENSATING, SagaStatus.COMPENSATION_FAILED),
                updatedAt = cutoff,
            )
        if (candidates.isEmpty()) return

        logger.info("SagaWatchdog: {} saga(s) eligible for compensation retry", candidates.size)
        candidates.forEach { saga ->
            runCatching {
                sagaEngine.runCompensation(saga.id)
            }.onFailure { e ->
                logger.error("SagaWatchdog: compensation retry failed for saga '${saga.id}': ${e.message}", e)
            }
        }
    }
}
