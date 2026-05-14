package com.vertyll.veds.iam.infrastructure.config

import com.vertyll.veds.iam.application.saga.port.SagaProcessPort
import com.vertyll.veds.iam.infrastructure.persistence.repository.SagaJpaRepository
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
internal class SchedulingConfig(
    private val sagaJpaRepository: SagaJpaRepository,
    private val sagaProcessPort: SagaProcessPort,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val SAGA_CLEANUP_DAYS = 30L
        private const val STUCK_SAGA_THRESHOLD_HOURS = 24L
    }

    @Scheduled(cron = "0 15 2 * * ?")
    @Transactional
    fun cleanupOldSagas() {
        val cutoffDate = Instant.now().minus(SAGA_CLEANUP_DAYS, ChronoUnit.DAYS)

        logger.info("Cleaning up sagas completed before {}", cutoffDate)

        val statuses = listOf(SagaStatus.COMPLETED, SagaStatus.COMPENSATED)
        val oldSagas = sagaJpaRepository.findByStatusInAndStartedAtBefore(statuses, cutoffDate)

        if (oldSagas.isNotEmpty()) {
            logger.info("Found {} old sagas to clean up", oldSagas.size)
            sagaJpaRepository.deleteAll(oldSagas)
            logger.info("Successfully cleaned up {} old sagas", oldSagas.size)
        } else {
            logger.info("No old sagas found to clean up")
        }
    }

    @Scheduled(cron = "0 20 * * * ?")
    @Transactional
    fun checkForStuckSagas() {
        val cutoffDate = Instant.now().minus(STUCK_SAGA_THRESHOLD_HOURS, ChronoUnit.HOURS)

        logger.info("Checking for stuck sagas started before {}", cutoffDate)

        val stuckSagas =
            sagaJpaRepository.findByStatusInAndStartedAtBefore(
                listOf(SagaStatus.STARTED, SagaStatus.AWAITING_RESPONSE, SagaStatus.COMPENSATING),
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
                    sagaProcessPort.markSagaFailed(
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
