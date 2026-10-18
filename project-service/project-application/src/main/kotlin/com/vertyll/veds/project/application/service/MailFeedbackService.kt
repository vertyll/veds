package com.vertyll.veds.project.application.service

import com.vertyll.veds.project.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.project.application.port.outbound.UseCaseLogger
import com.vertyll.veds.shared.saga.SagaProcessPort

class MailFeedbackService(
    private val sagaProcessPort: SagaProcessPort,
    private val logger: UseCaseLogger,
) : MailFeedbackUseCase {
    override fun handleMailSent(
        sagaId: String?,
        to: String,
    ) {
        if (sagaId == null) {
            logger.debug("MailSent without sagaId - skipping saga step recording")
            return
        }
        val saga = sagaProcessPort.findSagaById(sagaId)
        if (saga == null) {
            logger.debug("Saga '{}' not owned by project-service - ignoring MailSentEvent", sagaId)
            return
        }
        logger.info("MailSentEvent for saga: {} (to: {})", sagaId, to)
        sagaProcessPort.markSagaCompleted(sagaId)
    }

    override fun handleMailFailed(
        sagaId: String?,
        to: String,
        error: String,
    ) {
        if (sagaId == null) {
            logger.debug("MailFailed without sagaId - skipping saga failure")
            return
        }
        val saga = sagaProcessPort.findSagaById(sagaId)
        if (saga == null) {
            logger.debug("Saga '{}' not owned by project-service - ignoring MailFailedEvent", sagaId)
            return
        }
        logger.warn("MailFailedEvent for saga: {} (to: {}, error: {})", sagaId, to, error)
        sagaProcessPort.markSagaFailed(sagaId, "Mail delivery failed: $error")
    }
}
