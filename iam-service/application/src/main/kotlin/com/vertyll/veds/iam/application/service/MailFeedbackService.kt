package com.vertyll.veds.iam.application.service

import com.vertyll.veds.iam.application.port.inbound.MailFeedbackUseCase
import com.vertyll.veds.iam.application.port.out.SagaProcessPort
import com.vertyll.veds.iam.application.saga.model.SagaTypes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Application service handling inbound mail-delivery feedback for IAM sagas.
 *
 * Owns the business rules around what to do when mail-service confirms
 * (`MailSentEvent`) or reports a failure (`MailFailedEvent`) for an IAM saga.
 *
 * Driven by an inbound Kafka adapter; technology-agnostic by contract.
 */
@Service
internal class MailFeedbackService(
    private val sagaProcessPort: SagaProcessPort,
) : MailFeedbackUseCase {
    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        /**
         * Saga types where the saga should NOT be completed on mail delivery —
         * they require an additional user confirmation step.
         */
        val SAGA_TYPES_AWAITING_USER_CONFIRMATION =
            setOf(
                SagaTypes.EMAIL_CHANGE.value,
                SagaTypes.PASSWORD_CHANGE.value,
            )
    }

    @Transactional
    override fun handleMailSent(
        sagaId: String?,
        to: String,
    ) {
        if (sagaId == null) {
            logger.debug("MailSent without sagaId — skipping saga step recording")
            return
        }
        logger.info("MailSentEvent for saga: {} (to: {})", sagaId, to)

        val saga = sagaProcessPort.findSagaDomainById(sagaId)
        if (saga == null) {
            logger.warn("Saga '{}' not found — skipping MailSentEvent", sagaId)
            return
        }
        if (saga.type in SAGA_TYPES_AWAITING_USER_CONFIRMATION) {
            logger.info(
                "Mail delivered for saga '{}' (type: {}) — saga remains AWAITING_RESPONSE until user confirms",
                sagaId,
                saga.type,
            )
            return
        }
        sagaProcessPort.markSagaCompleted(sagaId)
    }

    @Transactional
    override fun handleMailFailed(
        sagaId: String?,
        to: String,
        error: String,
    ) {
        if (sagaId == null) {
            logger.debug("MailFailed without sagaId — skipping saga failure")
            return
        }
        logger.warn("MailFailedEvent for saga: {} (to: {}, error: {})", sagaId, to, error)
        sagaProcessPort.markSagaFailed(sagaId, "Mail delivery failed: $error")
    }
}
