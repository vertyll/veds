package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.domain.repository.SagaRepository
import com.vertyll.veds.iam.domain.service.SagaManager
import com.vertyll.veds.sharedinfrastructure.saga.enums.SagaStatus
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Configuration
@EnableScheduling
class SchedulingConfig(
    private val sagaRepository: SagaRepository,
    private val sagaManager: SagaManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val SAGA_CLEANUP_DAYS = 30L
        private const val STUCK_SAGA_THRESHOLD_HOURS = 24L
    }

    /**
     * Clean up old sagas that are completed or compensated
     * Runs daily at 2:15 AM
     */
    @Scheduled(cron = "0 15 2 * * ?")
    @Transactional
    fun cleanupOldSagas() {
        val cutoffDate = Instant.now().minus(SAGA_CLEANUP_DAYS, ChronoUnit.DAYS)

        logger.info("Cleaning up sagas completed before {}", cutoffDate)

        val statuses = listOf(SagaStatus.COMPLETED, SagaStatus.COMPENSATED)
        val oldSagas = sagaRepository.findByStatusInAndStartedAtBefore(statuses, cutoffDate)

        if (oldSagas.isNotEmpty()) {
            logger.info("Found {} old sagas to clean up", oldSagas.size)
            sagaRepository.deleteAll(oldSagas)
            logger.info("Successfully cleaned up {} old sagas", oldSagas.size)
        } else {
            logger.info("No old sagas found to clean up")
        }
    }

    /**
     * Check for stuck sagas and automatically compensate for them.
     * A saga is considered stuck if it has been in STARTED or COMPENSATING
     * status for longer than [STUCK_SAGA_THRESHOLD_HOURS].
     * Runs every hour at 20 minutes past the hour
     */
    @Scheduled(cron = "0 20 * * * ?")
    @Transactional
    fun checkForStuckSagas() {
        val cutoffDate = Instant.now().minus(STUCK_SAGA_THRESHOLD_HOURS, ChronoUnit.HOURS)

        logger.info("Checking for stuck sagas started before {}", cutoffDate)

        val stuckSagas =
            sagaRepository.findByStatusInAndStartedAtBefore(
                listOf(SagaStatus.STARTED, SagaStatus.COMPENSATING),
                cutoffDate,
            )

        if (stuckSagas.isNotEmpty()) {
            logger.warn("Found {} potentially stuck sagas — triggering auto-compensation", stuckSagas.size)
            stuckSagas.forEach { saga ->
                logger.warn(
                    "Compensating stuck saga: ID={}, Type={}, Status={}, StartedAt={}",
                    saga.id,
                    saga.type,
                    saga.status,
                    saga.startedAt,
                )
                try {
                    sagaManager.failSaga(
                        saga.id,
                        "Saga timed out after $STUCK_SAGA_THRESHOLD_HOURS hours without completion",
                    )
                    logger.info("Successfully compensated stuck saga: {}", saga.id)
                } catch (e: Exception) {
                    logger.error("Failed to compensate stuck saga {}: {}", saga.id, e.message, e)
                }
            }
        } else {
            logger.info("No stuck sagas found")
        }
    }
}
