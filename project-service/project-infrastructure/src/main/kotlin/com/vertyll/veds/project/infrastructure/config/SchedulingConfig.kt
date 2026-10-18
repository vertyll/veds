package com.vertyll.veds.project.infrastructure.config

import com.vertyll.veds.project.application.port.inbound.command.ProjectInvitationCommandUseCase
import com.vertyll.veds.project.infrastructure.persistence.repository.SagaJpaRepository
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStatus
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
    private val invitationService: ProjectInvitationCommandUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        private const val SAGA_CLEANUP_DAYS = 30L
        private const val STUCK_SAGA_THRESHOLD_HOURS = 24L
    }

    @Scheduled(cron = "0 */30 * * * ?")
    fun expireOverdueInvitations() {
        val expired = invitationService.expireOverdueInvitations()
        logger.debug("Invitation expiry sweep finished, {} expired", expired)
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
        } else {
            logger.info("No old sagas found to clean up")
        }
    }

    @Scheduled(cron = "0 20 * * * ?")
    @Transactional
    fun checkForStuckSagas() {
        val cutoffDate = Instant.now().minus(STUCK_SAGA_THRESHOLD_HOURS, ChronoUnit.HOURS)

        val stuckSagas =
            sagaJpaRepository.findByStatusInAndStartedAtBefore(
                listOf(SagaStatus.STARTED, SagaStatus.AWAITING_RESPONSE, SagaStatus.COMPENSATING),
                cutoffDate,
            )

        if (stuckSagas.isEmpty()) {
            logger.info("No stuck sagas found")
            return
        }

        logger.warn("Found {} potentially stuck sagas - triggering auto-compensation", stuckSagas.size)
        stuckSagas.forEach { saga ->
            try {
                sagaProcessPort.markSagaFailed(
                    saga.id,
                    "Saga timed out after $STUCK_SAGA_THRESHOLD_HOURS hours without completion",
                )
            } catch (e: Exception) {
                logger.error("Failed to compensate stuck saga {}: {}", saga.id, e.message, e)
            }
        }
    }
}
