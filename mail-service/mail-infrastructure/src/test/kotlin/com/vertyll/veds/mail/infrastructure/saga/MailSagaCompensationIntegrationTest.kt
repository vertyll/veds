package com.vertyll.veds.mail.infrastructure.saga

import com.vertyll.veds.mail.application.saga.model.SagaStepNames
import com.vertyll.veds.mail.application.saga.model.SagaTypes
import com.vertyll.veds.mail.infrastructure.IntegrationTestBase
import com.vertyll.veds.mail.infrastructure.config.SagaConfig
import com.vertyll.veds.shared.messaging.kafka.persistence.outbox.OutboxRepository
import com.vertyll.veds.shared.saga.SagaProcessPort
import com.vertyll.veds.shared.saga.SagaStepStatus
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

internal class MailSagaCompensationIntegrationTest(
    private val sagaProcess: SagaProcessPort,
    private val outbox: OutboxRepository,
) : IntegrationTestBase() {
    @Test
    fun `a failed saga asks to undo each step it completed`() {
        val sagaId = sagaProcess.startSaga(SagaTypes.EMAIL_SENDING, mapOf("to" to "reader@example.com")).id
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.SEND_EMAIL,
            status = SagaStepStatus.COMPLETED,
            payload = mapOf("emailId" to sagaId, "to" to "reader@example.com"),
        )
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.RECORD_EMAIL_LOG,
            status = SagaStepStatus.COMPLETED,
            payload = mapOf("logId" to 42L),
        )
        sagaProcess.recordSagaStep(
            sagaId = sagaId,
            stepName = SagaStepNames.TEMPLATE_UPDATE,
            status = SagaStepStatus.COMPLETED,
            payload = mapOf("templateName" to "TASK_CREATED"),
        )

        sagaProcess.markSagaFailed(sagaId, "the delivery was refused")

        await().atMost(TIMEOUT).until { compensationsOf(sagaId).size >= 3 }
        assertEquals(setOf(SagaConfig.SAGA_COMPENSATION_TOPIC), compensationsOf(sagaId).map { it.topic }.toSet())
    }

    private fun compensationsOf(sagaId: String) =
        outbox.findAll().filter { it.sagaId == sagaId && it.topic == SagaConfig.SAGA_COMPENSATION_TOPIC }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
